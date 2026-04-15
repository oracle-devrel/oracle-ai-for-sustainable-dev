#!/usr/bin/env python3

import argparse
import http.server
import json
import socketserver
import threading
import time
import urllib.parse
import urllib.request
import uuid
from pathlib import Path


DEFAULT_AUTHORIZE_URL = "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/authorize"
DEFAULT_TOKEN_URL = "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/token"


class CallbackState:
    def __init__(self, expected_state: str):
        self.expected_state = expected_state
        self.code = None
        self.error = None
        self.error_description = None
        self.received_state = None
        self.event = threading.Event()


def make_handler(callback_state: CallbackState, expected_path: str):
    class OAuthCallbackHandler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            parsed = urllib.parse.urlparse(self.path)
            if parsed.path != expected_path:
                self.send_response(404)
                self.send_header("Content-Type", "text/plain; charset=utf-8")
                self.end_headers()
                self.wfile.write(b"Not found.\n")
                return

            params = urllib.parse.parse_qs(parsed.query)
            callback_state.code = first(params.get("code"))
            callback_state.error = first(params.get("error"))
            callback_state.error_description = first(params.get("error_description"))
            callback_state.received_state = first(params.get("state"))
            callback_state.event.set()

            if callback_state.received_state != callback_state.expected_state:
                body = (
                    "<html><body><h2>State mismatch</h2>"
                    "<p>The OAuth callback state did not match. You can close this tab.</p>"
                    "</body></html>"
                )
            elif callback_state.error:
                body = (
                    "<html><body><h2>Authorization failed</h2>"
                    f"<p>{html_escape(callback_state.error)}</p>"
                    f"<p>{html_escape(callback_state.error_description or '')}</p>"
                    "<p>You can close this tab.</p>"
                    "</body></html>"
                )
            else:
                body = (
                    "<html><body><h2>Authorization received</h2>"
                    "<p>You can return to the terminal. The token exchange is in progress.</p>"
                    "</body></html>"
                )

            encoded = body.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)

        def log_message(self, format, *args):
            return

    return OAuthCallbackHandler


def first(values):
    if not values:
        return None
    return values[0]


def html_escape(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&#39;")
    )


def exchange_code_for_tokens(token_url: str, client_id: str, client_secret: str, code: str, redirect_uri: str):
    form = urllib.parse.urlencode(
        {
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": redirect_uri,
            "client_id": client_id,
            "client_secret": client_secret,
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        token_url,
        data=form,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = response.read().decode("utf-8")
        return json.loads(payload)


def update_env_file(env_path: Path, values: dict):
    existing_lines = []
    if env_path.exists():
        existing_lines = env_path.read_text().splitlines()

    keys = set(values.keys())
    new_lines = []
    seen = set()
    for line in existing_lines:
        if "=" in line and not line.lstrip().startswith("#"):
            key = line.split("=", 1)[0].strip()
            if key in values:
                new_lines.append(f"{key}={values[key]}")
                seen.add(key)
                continue
        new_lines.append(line)

    for key, value in values.items():
        if key not in seen:
            new_lines.append(f"{key}={value}")

    env_path.write_text("\n".join(new_lines) + "\n")


def main():
    parser = argparse.ArgumentParser(description="Mint an Oracle OAuth refresh token for the inventory gateway demo.")
    parser.add_argument("--client-id", required=True)
    parser.add_argument("--client-secret", required=True)
    parser.add_argument("--authorize-url", default=DEFAULT_AUTHORIZE_URL)
    parser.add_argument("--token-url", default=DEFAULT_TOKEN_URL)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--path", default="/oracle-oauth/callback")
    parser.add_argument("--scope", default="openid")
    parser.add_argument("--timeout-seconds", type=int, default=300)
    parser.add_argument("--write-env", help="Optional path to a .env file to update with gateway OAuth settings.")
    parser.add_argument("--output-json", help="Optional path to write the full token response JSON.")
    args = parser.parse_args()

    redirect_uri = f"http://{args.host}:{args.port}{args.path}"
    state = str(uuid.uuid4())
    callback_state = CallbackState(state)

    handler = make_handler(callback_state, args.path)
    with socketserver.TCPServer((args.host, args.port), handler) as server:
        server.timeout = 1
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()

        authorize_params = urllib.parse.urlencode(
            {
                "client_id": args.client_id,
                "redirect_uri": redirect_uri,
                "response_type": "code",
                "scope": args.scope,
                "state": state,
            }
        )
        authorize_url = f"{args.authorize_url}?{authorize_params}"

        print("Open this URL in your browser and sign in with the Oracle database demo user:")
        print(authorize_url)
        print()
        print(f"Waiting up to {args.timeout_seconds} seconds for the OAuth callback on {redirect_uri} ...")

        deadline = time.time() + args.timeout_seconds
        while time.time() < deadline:
            if callback_state.event.wait(timeout=1):
                break

        server.shutdown()

    if not callback_state.event.is_set():
        raise SystemExit("Timed out waiting for the Oracle OAuth callback.")

    if callback_state.received_state != callback_state.expected_state:
        raise SystemExit("OAuth callback state mismatch. Aborting.")

    if callback_state.error:
        description = callback_state.error_description or ""
        raise SystemExit(f"Oracle OAuth authorization failed: {callback_state.error} {description}".strip())

    if not callback_state.code:
        raise SystemExit("Oracle OAuth callback did not include an authorization code.")

    token_response = exchange_code_for_tokens(
        args.token_url,
        args.client_id,
        args.client_secret,
        callback_state.code,
        redirect_uri,
    )

    refresh_token = token_response.get("refresh_token")
    access_token = token_response.get("access_token")
    if not refresh_token:
        raise SystemExit("Token exchange succeeded, but no refresh_token was returned.")

    print()
    print("Oracle OAuth token exchange succeeded.")
    print(f"access_token: {access_token}")
    print(f"refresh_token: {refresh_token}")

    if args.output_json:
        output_path = Path(args.output_json).expanduser().resolve()
        output_path.write_text(json.dumps(token_response, indent=2) + "\n")
        print(f"Wrote token JSON to {output_path}")

    if args.write_env:
        env_path = Path(args.write_env).expanduser().resolve()
        update_env_file(
            env_path,
            {
                "ORACLE_AI_DATABASE_AGENT_CLIENT_ID": args.client_id,
                "ORACLE_AI_DATABASE_AGENT_CLIENT_SECRET": args.client_secret,
                "ORACLE_AI_DATABASE_AGENT_REFRESH_TOKEN": refresh_token,
            },
        )
        print(f"Updated env file: {env_path}")


if __name__ == "__main__":
    main()
