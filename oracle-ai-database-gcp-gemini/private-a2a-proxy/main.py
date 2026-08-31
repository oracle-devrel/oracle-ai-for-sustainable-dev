"""Private-network relay for an Oracle Autonomous AI Database A2A agent.

Gemini Enterprise reaches this service over HTTPS. The relay forwards the
caller's Oracle OAuth bearer token and an allowlisted A2A JSON-RPC request to
the database's private A2A endpoint through Cloud Run Direct VPC egress.
"""

from __future__ import annotations

import json
import os
from contextlib import asynccontextmanager
from urllib.parse import urlparse

import httpx
from fastapi import FastAPI, Header, HTTPException, Request, Response
from fastapi.responses import JSONResponse


PRIVATE_ORACLE_A2A_URL = os.environ.get("PRIVATE_ORACLE_A2A_URL", "").rstrip("/")
PUBLIC_A2A_URL = os.environ.get("PUBLIC_A2A_URL", "http://127.0.0.1:8080").rstrip("/")
ORACLE_AUTHORIZATION_URL = os.environ.get(
    "ORACLE_AUTHORIZATION_URL",
    "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/authorize",
)
ORACLE_TOKEN_URL = os.environ.get(
    "ORACLE_TOKEN_URL",
    "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/token",
)
MAX_REQUEST_BYTES = int(os.environ.get("MAX_REQUEST_BYTES", "1048576"))
UPSTREAM_TIMEOUT_SECONDS = float(os.environ.get("UPSTREAM_TIMEOUT_SECONDS", "120"))

ALLOWED_METHODS = frozenset({"message/send", "message/stream", "tasks/get", "tasks/cancel"})

def validate_configuration() -> None:
    if not PRIVATE_ORACLE_A2A_URL:
        raise RuntimeError("PRIVATE_ORACLE_A2A_URL is required")
    parsed = urlparse(PRIVATE_ORACLE_A2A_URL)
    if parsed.scheme != "https" or not parsed.hostname:
        raise RuntimeError("PRIVATE_ORACLE_A2A_URL must be an HTTPS URL")
    if not parsed.hostname.endswith(".oraclecloudapps.com"):
        raise RuntimeError("PRIVATE_ORACLE_A2A_URL must use an Oracle ADB hostname")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    validate_configuration()
    yield


app = FastAPI(
    title="Oracle AI Database Private A2A Relay",
    docs_url=None,
    redoc_url=None,
    lifespan=lifespan,
)


def agent_card() -> dict:
    return {
        "protocolVersion": "0.3.0",
        "name": "Oracle AI Database Agent via Private Network",
        "description": (
            "Queries governed Oracle AI Database data through the managed "
            "in-database agent over a private database endpoint."
        ),
        "url": PUBLIC_A2A_URL,
        "version": "1.0.0",
        "capabilities": {
            "streaming": False,
            "pushNotifications": False,
            "stateTransitionHistory": False,
        },
        "defaultInputModes": ["text/plain"],
        "defaultOutputModes": ["text/plain", "application/json"],
        "skills": [
            {
                "id": "oracle-database-analysis",
                "name": "Oracle AI Database Analysis",
                "description": (
                    "Uses a governed Select AI agent team to query and explain "
                    "authorized Oracle AI Database data."
                ),
                "tags": ["Oracle AI Database", "Select AI", "SQL", "analytics"],
            }
        ],
        "securitySchemes": {
            "oauth2": {
                "type": "oauth2",
                "flows": {
                    "authorizationCode": {
                        "authorizationUrl": ORACLE_AUTHORIZATION_URL,
                        "tokenUrl": ORACLE_TOKEN_URL,
                        "refreshUrl": ORACLE_TOKEN_URL,
                        "scopes": {"openid": "Authenticate as an authorized database user."},
                    }
                },
            }
        },
        "security": [{"oauth2": ["openid"]}],
        "preferredTransport": "JSONRPC",
    }


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.get("/.well-known/agent-card.json")
@app.get("/agent-card.json")
async def get_agent_card() -> dict:
    return agent_card()


@app.post("/")
async def relay(
    request: Request,
    authorization: str | None = Header(default=None),
) -> Response:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Oracle OAuth bearer token required")

    body = await request.body()
    if len(body) > MAX_REQUEST_BYTES:
        raise HTTPException(status_code=413, detail="A2A request is too large")

    try:
        payload = json.loads(body)
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=400, detail="A2A request must be valid JSON") from exc

    method = payload.get("method") if isinstance(payload, dict) else None
    if method not in ALLOWED_METHODS:
        raise HTTPException(status_code=400, detail="A2A method is not allowed")

    headers = {
        "Authorization": authorization,
        "Accept": "application/json, text/event-stream",
        "Content-Type": "application/json",
    }
    request_id = request.headers.get("x-request-id")
    if request_id:
        headers["X-Request-Id"] = request_id[:128]

    async with httpx.AsyncClient(timeout=UPSTREAM_TIMEOUT_SECONDS) as client:
        upstream = await client.post(PRIVATE_ORACLE_A2A_URL, content=body, headers=headers)

    content_type = upstream.headers.get("content-type", "application/json")
    return Response(content=upstream.content, status_code=upstream.status_code, media_type=content_type)


@app.exception_handler(httpx.RequestError)
async def upstream_failure(_request: Request, exception: httpx.RequestError) -> JSONResponse:
    # Do not disclose the private hostname or bearer token in the public error.
    return JSONResponse(
        status_code=502,
        content={"detail": "Private Oracle A2A endpoint is unavailable", "type": type(exception).__name__},
    )
