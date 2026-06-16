# Quick Start Guide - Oracle AI Database Agents

## 🚨 First: Fix Authentication Issue

All agents are failing with: `Reauthentication is needed`

**Run this on your GCP instance:**
```bash
gcloud auth application-default login --no-launch-browser
```

Follow the prompts to authenticate.

---

## ✅ Working Solutions

### 1. RAG-Only Agent (SIMPLEST - Start Here)
**File:** `oracle_ai_database_rag_only.py`  
**What it does:** Only queries Oracle Database documentation (no MCP, no database connections)

```bash
./run_rag_only.sh
```

**Use when:**
- You only need documentation search
- Want the most reliable solution
- Authentication is fixed

---

### 2. GenAI + Manual MCP (RECOMMENDED for MCP)
**File:** `oracle_ai_database_genai_mcp.py`  
**What it does:** Both documentation search AND direct database queries via MCP

```bash
./run_genai_mcp_agent.sh
```

**Features:**
- ✅ Works (you confirmed it runs)
- ✅ Now smarter - autonomous connection handling
- ✅ Don't need to specify connection names manually
- ✅ Just ask "show me tables" and it figures it out

**Updated behavior:**
- Agent automatically lists connections
- Picks the first/best one automatically
- Connects and queries without asking you for details

**Example queries:**
```
You: show me all tables
Agent: [Lists connections, connects automatically, shows tables]

You: what tables have JSON data
Agent: [Connects, queries schema, shows results]

You: what is vector search?
Agent: [Searches documentation knowledge base]
```

---

## ❌ Not Working

### 3. ADK with McpToolset
**File:** `oracle_ai_database_adk_agent.py`  
**Status:** Still broken with schema converter bug

**Error:** `AttributeError: 'list' object has no attribute 'items'`

This is a bug in ADK v1.22.1 that we cannot work around. See bug report below.

---

## 🐛 Bug Report for Google

**Where to file:** https://github.com/google/adk-python/issues

**What to attach:**
1. [BUG_REPORT_ADK_MCP.md](BUG_REPORT_ADK_MCP.md) - Full bug details
2. [adk_mcp_bug_reproducer.py](../adk_mcp_bug_reproducer.py) - Minimal reproducer script

**Bug title:** "McpToolset schema converter fails with array properties in Oracle SQLcl MCP"

**Reproducer usage:**
```bash
python adk_mcp_bug_reproducer.py
```

This will reproduce the exact error with minimal code.

---

## 📋 File Overview

| File | Purpose | Status |
|------|---------|--------|
| `oracle_ai_database_rag_only.py` | Documentation search only | ✅ Ready (after auth fix) |
| `oracle_ai_database_genai_mcp.py` | Documentation + MCP database | ✅ Works + improved |
| `oracle_ai_database_adk_agent.py` | ADK with McpToolset | ❌ Broken (ADK bug) |
| `oracle_ai_database_adk_fullagent.py` | Original RAG-only (ADK) | ⚠️ Needs auth fix |
| `run_rag_only.sh` | Launch RAG-only | ✅ Ready |
| `run_genai_mcp_agent.sh` | Launch GenAI+MCP | ✅ Ready |
| `run_agent.sh` | Launch ADK version | ❌ Will fail |
| `adk_mcp_bug_reproducer.py` | Bug reproducer for Google | 📄 For bug report |
| `BUG_REPORT_ADK_MCP.md` | Bug report document | 📄 For bug report |

---

## 🎯 Recommended Next Steps

1. **Fix auth issue:**
   ```bash
   gcloud auth application-default login --no-launch-browser
   ```

2. **Test RAG-only (simplest):**
   ```bash
   ./run_rag_only.sh
   ```

3. **Test GenAI+MCP (your tutorial requirement):**
   ```bash
   ./run_genai_mcp_agent.sh
   ```
   Try: "show me all tables" (should auto-connect now)

4. **File bug with Google:**
   - Go to: https://github.com/google/adk-python/issues/new
   - Title: "McpToolset schema converter fails with array properties in Oracle SQLcl MCP"
   - Attach: `BUG_REPORT_ADK_MCP.md` and `adk_mcp_bug_reproducer.py`

---

## 💡 For Your Tutorial

**Use:** `oracle_ai_database_genai_mcp.py`

**Why:**
- ✅ Includes MCP (your requirement)
- ✅ Actually works
- ✅ Now autonomous (smarter than before)
- ✅ Combines both RAG and database queries

**Tutorial flow:**
1. Show documentation search: "What is Oracle vector search?"
2. Show database queries: "Show me my tables"
3. Show combined: "What is JSON Duality and show me JSON tables in my database"

The agent will handle all the connection logic automatically!
