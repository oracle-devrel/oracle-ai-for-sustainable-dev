package com.oracle.demo.interactiveai;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;
import java.util.Properties;

/** Read-only diagnostics for the managed Oracle AI Database Agent setup. */
public final class OracleAgentDiagnostics {
    private OracleAgentDiagnostics() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> environment = System.getenv();
        String url = required(environment, "DB_URL");
        String username = required(environment, "DB_USERNAME");
        String password = required(environment, "DB_PASSWORD");

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        try (Connection connection = DriverManager.getConnection(url, properties)) {
            printRows(connection, "Database identity", """
                    select sys_context('USERENV', 'DB_UNIQUE_NAME') database_name,
                           sys_context('USERENV', 'SERVICE_NAME') service_name,
                           sys_context('USERENV', 'SESSION_USER') session_user
                    from dual
                    """);
            printRows(connection, "Oracle agent team", """
                    select agent_team_name, status
                    from user_ai_agent_teams
                    where agent_team_name = 'ORACLE_AI_DATABASE_AGENT'
                    """);
            printRows(connection, "Oracle agent tools", """
                    select tool_name
                    from user_ai_agent_tools
                    where tool_name in (
                      'SQL_TOOL', 'DISTINCT_VALUES_CHECK',
                      'RANGE_VALUES_CHECK', 'GENERATE_CHART'
                    )
                    order by tool_name
                    """);
            printRows(connection, "Select AI profiles", """
                    select profile_name, status
                    from user_cloud_ai_profiles
                    where profile_name in (
                      'PAULPARK_SUPPLY_CHAIN_OPENAI',
                      'PAULPARK_SUPPLY_CHAIN_GOOGLE'
                    )
                    order by profile_name
                    """);

            if (Boolean.parseBoolean(environment.getOrDefault(
                    "RUN_ORACLE_AGENT_QUERY", "false"))) {
                runAgent(connection);
            }
        }
    }

    private static void runAgent(Connection connection) throws Exception {
        String block = """
                declare
                  l_conversation_id varchar2(128);
                begin
                  l_conversation_id := dbms_cloud_ai.create_conversation(
                    attributes => '{"title":"Managed-agent diagnostic","retention_days":1,"conversation_length":4}'
                  );
                  ? := dbms_cloud_ai_agent.run_team(
                    team_name   => 'ORACLE_AI_DATABASE_AGENT',
                    user_prompt => 'Return the names of up to three products in the inventory demo.',
                    params      => json_object('conversation_id' value l_conversation_id)
                  );
                end;
                """;
        try (CallableStatement statement = connection.prepareCall(block)) {
            statement.registerOutParameter(1, Types.CLOB);
            statement.execute();
            Clob response = statement.getClob(1);
            String text = response == null
                    ? "<null>"
                    : response.getSubString(1, (int) Math.min(response.length(), 2_000));
            System.out.println("Managed agent invocation: SUCCESS");
            System.out.println(text);
        }
    }

    private static void printRows(
            Connection connection,
            String label,
            String sql) {
        System.out.println("== " + label + " ==");
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            int columns = rows.getMetaData().getColumnCount();
            int count = 0;
            while (rows.next()) {
                count++;
                StringBuilder line = new StringBuilder();
                for (int column = 1; column <= columns; column++) {
                    if (column > 1) line.append(" | ");
                    line.append(rows.getMetaData().getColumnLabel(column))
                            .append('=')
                            .append(rows.getString(column));
                }
                System.out.println(line);
            }
            if (count == 0) System.out.println("NO ROWS");
        } catch (SQLException exception) {
            System.out.println("FAILED ORA-" + exception.getErrorCode()
                    + ": " + exception.getMessage());
        }
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
