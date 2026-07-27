#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

toolkit_commit="5bb406b5b70e109a749cd16cc026422134a117b2"
runtime_root="../.runtime"
checkout_root="$runtime_root/oracle-mcp"
toolkit_root="$checkout_root/src/oracle-db-mcp-java-toolkit"
toolkit_jar="$toolkit_root/target/oracle-db-mcp-toolkit-1.0.0.jar"
toolkit_source="$toolkit_root/src/main/java/com/oracle/database/mcptoolkit/OracleDatabaseMCPToolkit.java"
stdio_patch="$PWD/../oracle-db-mcp-toolkit/patches/stdio-tool-registration.patch"

if [ ! -d "$checkout_root/.git" ]; then
  echo "Downloading the pinned Oracle Database MCP Java Toolkit source..." >&2
  git clone --quiet https://github.com/oracle/mcp.git "$checkout_root"
fi

current_commit="$(git -C "$checkout_root" rev-parse HEAD)"
if [ "$current_commit" != "$toolkit_commit" ]; then
  git -C "$checkout_root" fetch --quiet origin "$toolkit_commit"
  git -C "$checkout_root" checkout --quiet --detach "$toolkit_commit"
fi

if git -C "$checkout_root" apply --check "$stdio_patch" 2>/dev/null; then
  git -C "$checkout_root" apply "$stdio_patch"
elif ! git -C "$checkout_root" apply --reverse --check "$stdio_patch" 2>/dev/null; then
  echo "Unable to apply the documented Toolkit stdio compatibility patch." >&2
  exit 1
fi

if [ ! -f "$toolkit_jar" ] || [ "$toolkit_source" -nt "$toolkit_jar" ]; then
  echo "Building Oracle Database MCP Java Toolkit $toolkit_commit..." >&2
  if command -v mvn >/dev/null 2>&1; then
    maven_command="mvn"
  elif [ -x /opt/homebrew/opt/maven/bin/mvn ]; then
    maven_command="/opt/homebrew/opt/maven/bin/mvn"
  else
    echo "Maven 3.9+ is required." >&2
    exit 1
  fi
  # Keep stdout reserved for the single JAR path consumed by run.sh.
  "$maven_command" -q -f "$toolkit_root/pom.xml" -DskipTests package >&2
fi

cd "$toolkit_root"
printf '%s/target/oracle-db-mcp-toolkit-1.0.0.jar\n' "$PWD"
