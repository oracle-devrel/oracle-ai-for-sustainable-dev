#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# shellcheck disable=SC1091
source ./load-database-env.sh

if [ -z "${ORACLE_MCP_TOOLKIT_JAR:-}" ]; then
  ORACLE_MCP_TOOLKIT_JAR="$(./prepare-mcp-toolkit.sh)"
  export ORACLE_MCP_TOOLKIT_JAR
fi

java_command="${JAVA_COMMAND:-java}"

# A locally cached Toolkit may have been built by a newer JDK. Select a
# compatible installed JDK for both the agent and its MCP child process.
if command -v javap >/dev/null 2>&1; then
  runtime_class_version="$("$java_command" -XshowSettings:properties -version 2>&1 \
    | awk '/java.class.version =/ {print int($3); exit}')"
  toolkit_class_version="$(javap -verbose -classpath "$ORACLE_MCP_TOOLKIT_JAR" \
    com.oracle.database.mcptoolkit.OracleDatabaseMCPToolkit 2>/dev/null \
    | awk '/major version:/ {print $3; exit}')"
  if [ -n "$runtime_class_version" ] && [ -n "$toolkit_class_version" ] \
      && [ "$toolkit_class_version" -gt "$runtime_class_version" ]; then
    required_java_version="$((toolkit_class_version - 44))"
    if [ -x /usr/libexec/java_home ]; then
      compatible_java_home="$(/usr/libexec/java_home -v "$required_java_version+" 2>/dev/null || true)"
    else
      compatible_java_home=""
    fi
    if [ -z "$compatible_java_home" ] || [ ! -x "$compatible_java_home/bin/java" ]; then
      echo "The cached Oracle MCP Toolkit requires Java $required_java_version, but the current runtime supports class version $runtime_class_version." >&2
      echo "Install JDK $required_java_version or newer, or delete .runtime/oracle-mcp and rerun to rebuild the Toolkit." >&2
      exit 1
    fi
    java_command="$compatible_java_home/bin/java"
    export JAVA_HOME="$compatible_java_home"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using Java $required_java_version for the cached Oracle MCP Toolkit." >&2
  fi
fi

if command -v mvn >/dev/null 2>&1; then
  maven_command="mvn"
elif [ -x /opt/homebrew/opt/maven/bin/mvn ]; then
  maven_command="/opt/homebrew/opt/maven/bin/mvn"
else
  echo "Maven 3.9+ is required." >&2
  exit 1
fi

"$maven_command" -q -DskipTests package
"$java_command" -Dweb.root=../web-client -jar target/interactive-ai-agent-service-0.1.0-SNAPSHOT.jar
