package com.oracle.demo.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import oracle.jdbc.OracleConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trace")
public class TraceController {

  private static final String TRACEPARENT_CLIENT_INFO_NAME = "clientcontext.ora$opentelem$tracectx";
  private static final String TRACELEVEL_CLIENT_INFO_NAME = "clientcontext.ora$opentelem$tracelevel";
  private static final Pattern SQL_ID_PATTERN = Pattern.compile("SQL_ID\\s+([a-z0-9]+)", Pattern.CASE_INSENSITIVE);

  private final DataSource dataSource;
  private final ObservationRegistry observationRegistry;
  private final Tracer tracer;
  private final boolean serverTelemetryTracesEnabled;
  private final boolean serverTelemetryLoggingEnabled;
  private final boolean traceparentClientInfoEnabled;
  private final boolean tracelevelClientInfoEnabled;

  public TraceController(
      DataSource dataSource,
      ObservationRegistry observationRegistry,
      Tracer tracer,
      @Value("${demo.oracle.server-telemetry.traces-enabled:true}") boolean serverTelemetryTracesEnabled,
      @Value("${demo.oracle.server-telemetry.logging-enabled:false}") boolean serverTelemetryLoggingEnabled,
      @Value("${demo.oracle.server-telemetry.traceparent-client-info-enabled:true}") boolean traceparentClientInfoEnabled,
      @Value("${demo.oracle.server-telemetry.tracelevel-client-info-enabled:true}") boolean tracelevelClientInfoEnabled) {
    this.dataSource = dataSource;
    this.observationRegistry = observationRegistry;
    this.tracer = tracer;
    this.serverTelemetryTracesEnabled = serverTelemetryTracesEnabled;
    this.serverTelemetryLoggingEnabled = serverTelemetryLoggingEnabled;
    this.traceparentClientInfoEnabled = traceparentClientInfoEnabled;
    this.tracelevelClientInfoEnabled = tracelevelClientInfoEnabled;
  }

  @GetMapping("/roundtrip")
  public Map<String, Object> roundtrip() {
    return Observation
        .createNotStarted("oracle.demo.database-roundtrip", observationRegistry)
        .lowCardinalityKeyValue("db.system", "oracle")
        .observe(this::queryDatabase);
  }

  @GetMapping("/agent-task")
  public Map<String, Object> agentTask(
      @RequestParam(defaultValue = "sustainability-risk-agent") String agentId,
      @RequestParam(defaultValue = "investigate suspicious high-value payment patterns") String task) {
    String sanitizedAgentId = sanitizeAgentId(agentId);
    return Observation
        .createNotStarted("oracle.demo.agent-database-investigation", observationRegistry)
        .lowCardinalityKeyValue("db.system", "oracle")
        .lowCardinalityKeyValue("agent.id", sanitizedAgentId)
        .observe(() -> queryAgentTask(sanitizedAgentId, task));
  }

  @GetMapping(value = "/agent-task/view", produces = "text/html")
  public String agentTaskView(
      @RequestParam(defaultValue = "claims-investigator-agent") String agentId,
      @RequestParam(defaultValue = "investigate_payment_anomalies") String task,
      @RequestParam(defaultValue = "http://localhost:16686") String jaegerUrl) {
    Map<String, Object> result = agentTask(agentId, task);
    return renderAgentTask(result, jaegerUrl);
  }

  private Map<String, Object> queryDatabase() {
    Map<String, String> trace = currentTrace();

    try (Connection connection = dataSource.getConnection()) {
      OracleConnection oracleConnection = connection.unwrap(OracleConnection.class);
      oracleConnection.beginRequest();
      try {
        configureTraceContext(oracleConnection, trace);
        configureDatabaseSession(oracleConnection, trace, "springboot-oracle-db-otel-demo", "roundtrip-start");
        toggleServerTelemetry(oracleConnection);

        configureDatabaseAction(oracleConnection, "context-query");
        Map<String, Object> databaseContext = queryForMap(connection, """
            select
              sys_context('USERENV', 'CURRENT_SCHEMA') as schema_name,
              sys_context('USERENV', 'SERVICE_NAME') as service_name,
              sys_context('USERENV', 'DB_NAME') as database_name,
              sys_context('USERENV', 'MODULE') as module_name,
              sys_context('USERENV', 'ACTION') as action_name,
              sys_context('USERENV', 'CLIENT_IDENTIFIER') as client_identifier,
              sys_context('USERENV', 'SID') as session_id
            from dual
            """);

        configureDatabaseAction(oracleConnection, "metadata-query");
        Number tableCount = queryForNumber(connection, "select count(*) from user_tables");
        Map<String, Object> workload = runEnrichedWorkload(connection);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("trace", trace);
        response.put("database", databaseContext);
        response.put("userTableCount", tableCount == null ? 0 : tableCount.longValue());
        response.put("workload", workload);
        response.put("serverTelemetry", Map.of(
            "tracesEnabled", serverTelemetryTracesEnabled,
            "loggingEnabled", serverTelemetryLoggingEnabled,
            "traceparentClientInfoEnabled", traceparentClientInfoEnabled,
            "tracelevelClientInfoEnabled", tracelevelClientInfoEnabled));
        response.put("message", "Check Jaeger for the HTTP span, Micrometer observation, Oracle JDBC roundtrips, and the oracle-db DB Server span with module/action/session metadata.");
        return response;
      } finally {
        clearDatabaseSession(oracleConnection);
        oracleConnection.endRequest();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Oracle database trace roundtrip failed", e);
    }
  }

  private Map<String, Object> queryAgentTask(String agentId, String task) {
    Map<String, String> trace = currentTrace();
    String operationName = "agent-" + shortValue(agentId, 24);
    Long operationExecutionId = null;

    try (Connection connection = dataSource.getConnection()) {
      OracleConnection oracleConnection = connection.unwrap(OracleConnection.class);
      oracleConnection.beginRequest();
      try {
        configureTraceContext(oracleConnection, trace);
        configureDatabaseSession(oracleConnection, trace, "agent:" + agentId, "agent-task-start");
        toggleServerTelemetry(oracleConnection);
        ensureAgentEventTable(connection);
        operationExecutionId = beginMonitoredOperation(connection, operationName, agentId, task, trace);

        configureDatabaseAction(oracleConnection, "agent-workload-query");
        AgentWorkloadResult agentResult = runAgentWorkload(connection, agentId);

        List<String> executionPlan = tryDisplayCursorPlan(connection);
        String sqlId = extractSqlId(executionPlan);
        configureDatabaseAction(oracleConnection, "agent-plan-report");
        Map<String, Object> sqlStats = sqlId.isBlank()
            ? Map.of("available", false, "reason", "SQL_ID was not present in DBMS_XPLAN output")
            : trySqlStats(connection, sqlId);
        Map<String, Object> sqlDetails = sqlId.isBlank()
            ? Map.of("available", false, "reason", "SQL_ID was not present in DBMS_XPLAN output")
            : trySqlDetails(connection, sqlId);

        configureDatabaseAction(oracleConnection, "agent-sql-monitor");
        Map<String, Object> sqlMonitor = sqlId.isBlank()
            ? Map.of("available", false, "reason", "SQL_ID was not present in DBMS_XPLAN output")
            : trySqlMonitorReport(connection, sqlId);

        configureDatabaseAction(oracleConnection, "agent-session-summary");
        Map<String, Object> databaseContext = queryForMap(connection, """
            select
              sys_context('USERENV', 'CURRENT_SCHEMA') as schema_name,
              sys_context('USERENV', 'SERVICE_NAME') as service_name,
              sys_context('USERENV', 'DB_NAME') as database_name,
              sys_context('USERENV', 'MODULE') as module_name,
              sys_context('USERENV', 'ACTION') as action_name,
              sys_context('USERENV', 'CLIENT_IDENTIFIER') as client_identifier,
              sys_context('USERENV', 'SID') as session_id,
              sys_context('USERENV', 'INSTANCE_NAME') as instance_name,
              sys_context('USERENV', 'SERVER_HOST') as server_host
            from dual
            """);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("agent", Map.of(
            "id", agentId,
            "task", task,
            "databaseOperation", operationName,
            "databaseOperationExecutionId", operationExecutionId == null ? "" : operationExecutionId));
        response.put("trace", trace);
        response.put("database", databaseContext);
        response.put("workload", agentResult.summary());
        response.put("bindValues", agentResult.bindValues());
        response.put("sqlBridge", Map.of(
            "sqlId", sqlId,
            "jaegerAttribute", "oracle.db.query.sql.id",
            "canonicalSource", "DBMS_XPLAN and SQL Monitor",
            "description", "Use oracle.db.query.sql.id on the Jaeger DB Server span as the bridge from the visual trace to these Oracle SQL Monitor and DBMS_XPLAN details."));
        response.put("sqlStats", sqlStats);
        response.put("sqlDetails", sqlDetails);
        response.put("sqlMonitor", sqlMonitor);
        response.put("dbmsXplan", Map.of(
            "available", !executionPlan.isEmpty(),
            "format", "ALLSTATS LAST +IOSTATS +MEMSTATS",
            "lines", executionPlan));
        response.put("serverTelemetry", Map.of(
            "tracesEnabled", serverTelemetryTracesEnabled,
            "loggingEnabled", serverTelemetryLoggingEnabled,
            "traceparentClientInfoEnabled", traceparentClientInfoEnabled,
            "tracelevelClientInfoEnabled", tracelevelClientInfoEnabled));
        response.put("message", "Open the trace in Jaeger, expand the oracle-db / DB Server span, then use oracle.db.query.sql.id to connect the visual trace to SQL Monitor and DBMS_XPLAN.");
        return response;
      } finally {
        if (operationExecutionId != null) {
          endMonitoredOperation(connection, operationName, operationExecutionId);
        }
        clearDatabaseSession(oracleConnection);
        oracleConnection.endRequest();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Oracle database agent trace task failed", e);
    }
  }

  private void configureTraceContext(OracleConnection oracleConnection, Map<String, String> trace)
      throws SQLException {
    if (!traceparentClientInfoEnabled || trace.get("traceId").isBlank() || trace.get("spanId").isBlank()) {
      return;
    }
    oracleConnection.setClientInfo(
        TRACEPARENT_CLIENT_INFO_NAME,
        "traceparent: 00-" + trace.get("traceId") + "-" + trace.get("spanId") + "-01\r\n");
    if (tracelevelClientInfoEnabled) {
      oracleConnection.setClientInfo(TRACELEVEL_CLIENT_INFO_NAME, "tracelevel: 0x01");
    }
  }

  private void configureDatabaseSession(
      OracleConnection oracleConnection, Map<String, String> trace, String module, String action)
      throws SQLException {
    String[] metrics = new String[OracleConnection.END_TO_END_STATE_INDEX_MAX];
    metrics[OracleConnection.END_TO_END_MODULE_INDEX] = shortValue(module, 48);
    metrics[OracleConnection.END_TO_END_ACTION_INDEX] = shortValue(action, 32);
    metrics[OracleConnection.END_TO_END_CLIENTID_INDEX] = "traceId=" + shortValue(trace.get("traceId"));
    metrics[OracleConnection.END_TO_END_ECID_INDEX] = shortValue(trace.get("traceId"));
    oracleConnection.setEndToEndMetrics(metrics, (short) 0);
  }

  private Long beginMonitoredOperation(
      Connection connection, String operationName, String agentId, String task, Map<String, String> trace)
      throws SQLException {
    try (CallableStatement statement = connection.prepareCall("""
        begin
          ? := dbms_sql_monitor.begin_operation(
            dbop_name => ?,
            forced_tracking => dbms_sql_monitor.force_tracking,
            attribute_list => ?);
        end;
        """)) {
      statement.registerOutParameter(1, Types.NUMERIC);
      statement.setString(2, operationName);
      statement.setString(3, "agent=" + agentId + ",traceId=" + shortValue(trace.get("traceId"), 32)
          + ",task=" + attributeValue(task, 40));
      statement.execute();
      Number executionId = (Number) statement.getObject(1);
      return executionId == null ? null : executionId.longValue();
    }
  }

  private void endMonitoredOperation(Connection connection, String operationName, Long executionId) {
    try (CallableStatement statement = connection.prepareCall("""
        begin
          dbms_sql_monitor.end_operation(dbop_name => ?, dbop_eid => ?);
        end;
        """)) {
      statement.setString(1, operationName);
      statement.setLong(2, executionId);
      statement.execute();
    } catch (SQLException ignored) {
      // The demo response should still be returned even if SQL Monitor cleanup
      // fails because the operation already completed.
    }
  }

  private void configureDatabaseAction(Connection connection, String action) throws SQLException {
    configureDatabaseAction(connection.unwrap(OracleConnection.class), action);
  }

  private void configureDatabaseAction(OracleConnection oracleConnection, String action) throws SQLException {
    String[] metrics = oracleConnection.getEndToEndMetrics();
    if (metrics == null || metrics.length < OracleConnection.END_TO_END_STATE_INDEX_MAX) {
      metrics = new String[OracleConnection.END_TO_END_STATE_INDEX_MAX];
    }
    metrics[OracleConnection.END_TO_END_ACTION_INDEX] = shortValue(action, 32);
    oracleConnection.setEndToEndMetrics(metrics, (short) 0);
  }

  private void clearDatabaseSession(OracleConnection oracleConnection) throws SQLException {
    oracleConnection.setEndToEndMetrics(
        new String[OracleConnection.END_TO_END_STATE_INDEX_MAX], (short) 0);
  }

  private Map<String, Object> runEnrichedWorkload(Connection connection) throws SQLException {
    Map<String, Object> workload = new LinkedHashMap<>();

    configureDatabaseAction(connection, "cpu-shape-query");
    workload.put("syntheticRows", queryForNumber(connection, """
        select count(*)
        from (
          select level as id
          from dual
          connect by level <= 5000
        )
        """));

    configureDatabaseAction(connection, "dictionary-query");
    workload.put("dictionaryObjectsVisible", queryForNumber(connection, """
        select count(*)
        from all_objects
        where rownum <= 1000
        """));

    configureDatabaseAction(connection, "server-metadata-query");
    workload.put("serverMetadata", queryForMap(connection, """
        select
          sys_context('USERENV', 'MODULE') as module_name,
          sys_context('USERENV', 'ACTION') as action_name,
          sys_context('USERENV', 'CLIENT_IDENTIFIER') as client_identifier,
          sys_context('USERENV', 'INSTANCE_NAME') as instance_name,
          sys_context('USERENV', 'SERVER_HOST') as server_host
        from dual
        """));

    return workload;
  }

  private void ensureAgentEventTable(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("""
          create table agent_event_log (
            event_id number generated always as identity primary key,
            agent_id varchar2(128) not null,
            event_type varchar2(40) not null,
            customer_region varchar2(20) not null,
            amount number(12,2) not null,
            risk_score number(3) not null,
            created_at timestamp default systimestamp not null
          )
          """);
    } catch (SQLException e) {
      if (e.getErrorCode() != 955) {
        throw e;
      }
    }

    Number rowCount = queryForNumber(connection, "select count(*) from agent_event_log");
    if (rowCount != null && rowCount.longValue() > 0) {
      return;
    }

    try (PreparedStatement statement = connection.prepareStatement("""
        insert /*+ append */ into agent_event_log (
          agent_id, event_type, customer_region, amount, risk_score, created_at)
        select
          case mod(level, 3)
            when 0 then 'claims-investigator-agent'
            when 1 then 'sustainability-risk-agent'
            else 'payments-review-agent'
          end,
          case mod(level, 4)
            when 0 then 'payment'
            when 1 then 'claim'
            when 2 then 'transfer'
            else 'profile-update'
          end,
          case mod(level, 5)
            when 0 then 'EMEA'
            when 1 then 'NA'
            when 2 then 'APAC'
            when 3 then 'LATAM'
            else 'GLOBAL'
          end,
          round(50 + mod(level * 37, 20000) / 3, 2),
          mod(level * 17 + 23, 101),
          systimestamp - numtodsinterval(mod(level, 720), 'minute')
        from dual
        connect by level <= ?
        """)) {
      statement.setInt(1, 25000);
      statement.executeUpdate();
    }
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }

  private AgentWorkloadResult runAgentWorkload(Connection connection, String agentId) throws SQLException {
    int riskThreshold = 80;
    double minimumAmount = 1000.0;
    String eventType = "payment";
    try (PreparedStatement statement = connection.prepareStatement("""
        select /*+ monitor gather_plan_statistics agentic_ai_trace_bridge */
          count(*) as events_scanned,
          sum(case when risk_score >= ? then 1 else 0 end) as flagged_events,
          max(risk_score) as highest_risk_score,
          max(amount) as highest_amount,
          min(customer_region) keep (dense_rank first order by risk_score desc, amount desc) as sample_region
        from agent_event_log
        where agent_id = ?
          and event_type = ?
          and amount >= ?
        """)) {
      statement.setInt(1, riskThreshold);
      statement.setString(2, agentId);
      statement.setString(3, eventType);
      statement.setDouble(4, minimumAmount);
      try (ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        return new AgentWorkloadResult(Map.of(), Map.of());
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("eventsScanned", resultSet.getObject("EVENTS_SCANNED"));
      result.put("flaggedEvents", resultSet.getObject("FLAGGED_EVENTS"));
      result.put("highestRiskScore", resultSet.getObject("HIGHEST_RISK_SCORE"));
        result.put("highestAmount", resultSet.getObject("HIGHEST_AMOUNT"));
        result.put("sampleRegion", resultSet.getObject("SAMPLE_REGION"));
        result.put("interpretation", "The agent ran a bind-variable risk scan against AGENT_EVENT_LOG. The database server-side span for this SQL should expose oracle.db.query.sql.id, which links this trace to SQL Monitor, DBMS_XPLAN, full SQL text, and captured bind samples.");
        Map<String, Object> bindValues = new LinkedHashMap<>();
        bindValues.put("riskThreshold", riskThreshold);
        bindValues.put("agentId", agentId);
        bindValues.put("eventType", eventType);
        bindValues.put("minimumAmount", minimumAmount);
        return new AgentWorkloadResult(result, bindValues);
      }
    }
  }

  private List<String> tryDisplayCursorPlan(Connection connection) {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("""
            select plan_table_output
            from table(dbms_xplan.display_cursor(
              sql_id => null,
              cursor_child_no => null,
              format => 'ALLSTATS LAST +IOSTATS +MEMSTATS'))
            """)) {
      List<String> lines = new ArrayList<>();
      while (resultSet.next()) {
        lines.add(resultSet.getString(1));
      }
      return lines;
    } catch (SQLException e) {
      return List.of("DBMS_XPLAN unavailable: " + summarizeSqlException(e));
    }
  }

  private Map<String, Object> trySqlStats(Connection connection, String sqlId) {
    try (PreparedStatement statement = connection.prepareStatement("""
        select
          sql_id,
          plan_hash_value,
          executions,
          rows_processed,
          elapsed_time,
          cpu_time,
          buffer_gets,
          disk_reads,
          last_active_time
        from v$sql
        where sql_id = ?
        order by last_active_time desc
        fetch first 1 row only
        """)) {
      statement.setString(1, sqlId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Map.of("available", false, "reason", "No V$SQL row found for SQL_ID " + sqlId);
        }
        ResultSetMetaData metaData = resultSet.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("available", true);
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
        }
        return row;
      }
    } catch (SQLException e) {
      return Map.of("available", false, "reason", summarizeSqlException(e));
    }
  }

  private Map<String, Object> trySqlDetails(Connection connection, String sqlId) {
    try {
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("available", true);
      details.put("sqlId", sqlId);
      details.put("sqlText", querySqlText(connection, sqlId));
      details.put("bindCaptureNote", "Oracle exposes captured bind samples, not every bind value for every execution. This demo also returns the application bind values so the page has the exact values used for this run even when V$SQL_BIND_CAPTURE has no fresh sample.");
      details.put("bindSamples", queryBindSamples(connection, sqlId));
      return details;
    } catch (SQLException e) {
      return Map.of("available", false, "reason", summarizeSqlException(e));
    }
  }

  private String querySqlText(Connection connection, String sqlId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        select sql_fulltext
        from v$sql
        where sql_id = ?
        order by last_active_time desc
        fetch first 1 row only
        """)) {
      statement.setString(1, sqlId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return "";
        }
        return clobOrString(resultSet, 1);
      }
    }
  }

  private List<Map<String, Object>> queryBindSamples(Connection connection, String sqlId)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        select
          name,
          position,
          datatype_string,
          value_string,
          last_captured
        from v$sql_bind_capture
        where sql_id = ?
        order by position
        """)) {
      statement.setString(1, sqlId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        while (resultSet.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= metaData.getColumnCount(); i++) {
            row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
          }
          rows.add(row);
        }
        return rows;
      }
    }
  }

  private Map<String, Object> trySqlMonitorReport(Connection connection, String sqlId) {
    try (PreparedStatement statement = connection.prepareStatement("""
        select dbms_sql_monitor.report_sql_monitor(
          sql_id => ?,
          report_level => 'ALL',
          type => 'TEXT') as report
        from dual
        """)) {
      statement.setString(1, sqlId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Map.of("available", false, "reason", "SQL Monitor returned no report for SQL_ID " + sqlId);
        }
        String report = clobOrString(resultSet, 1);
        return Map.of(
            "available", true,
            "type", "TEXT",
            "sqlId", sqlId,
            "reportPreview", truncate(report, 12000));
      }
    } catch (SQLException e) {
      return Map.of("available", false, "reason", summarizeSqlException(e));
    }
  }

  private record AgentWorkloadResult(Map<String, Object> summary, Map<String, Object> bindValues) {}

  private void toggleServerTelemetry(OracleConnection oracleConnection) {
    EnumSet<OracleConnection.ServerTelemetry> requestedTelemetry =
        EnumSet.noneOf(OracleConnection.ServerTelemetry.class);
    if (serverTelemetryTracesEnabled) {
      requestedTelemetry.add(OracleConnection.ServerTelemetry.Traces);
    }
    if (serverTelemetryLoggingEnabled) {
      requestedTelemetry.add(OracleConnection.ServerTelemetry.Logging);
    }

    // Workaround for provider builds where the startup capability can look like
    // the current state: force a state change so the driver piggybacks it.
    oracleConnection.setServerTelemetry(EnumSet.noneOf(OracleConnection.ServerTelemetry.class));
    if (!requestedTelemetry.isEmpty()) {
      oracleConnection.setServerTelemetry(requestedTelemetry);
    }
  }

  private Map<String, Object> queryForMap(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      if (!resultSet.next()) {
        return Map.of();
      }
      ResultSetMetaData metaData = resultSet.getMetaData();
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 1; i <= metaData.getColumnCount(); i++) {
        row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
      }
      return row;
    }
  }

  private Number queryForNumber(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      if (!resultSet.next()) {
        return 0;
      }
      return (Number) resultSet.getObject(1);
    }
  }

  private Map<String, String> currentTrace() {
    Span span = tracer.currentSpan();
    if (span == null) {
      return Map.of("traceId", "", "spanId", "");
    }
    return Map.of(
        "traceId", span.context().traceId(),
        "spanId", span.context().spanId());
  }

  private String shortValue(String value) {
    return shortValue(value, 32);
  }

  private String shortValue(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "none";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String sanitizeAgentId(String agentId) {
    if (agentId == null || agentId.isBlank()) {
      return "sustainability-risk-agent";
    }
    String sanitized = agentId.replaceAll("[^A-Za-z0-9_.-]", "_");
    return shortValue(sanitized, 48);
  }

  private String attributeValue(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "none";
    }
    return shortValue(value.replaceAll("[,=\\r\\n]", "_"), maxLength);
  }

  private String extractSqlId(List<String> executionPlan) {
    for (String line : executionPlan) {
      Matcher matcher = SQL_ID_PATTERN.matcher(line);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return "";
  }

  private String clobOrString(ResultSet resultSet, int columnIndex) throws SQLException {
    Clob clob = resultSet.getClob(columnIndex);
    if (clob != null) {
      long length = Math.min(clob.length(), 12000);
      return clob.getSubString(1, (int) length);
    }
    String value = resultSet.getString(columnIndex);
    return value == null ? "" : value;
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxLength) + "\n... truncated ...";
  }

  private String summarizeSqlException(SQLException e) {
    return e.getErrorCode() == 0
        ? e.getMessage()
        : "ORA-" + String.format("%05d", e.getErrorCode()) + ": " + e.getMessage();
  }

  @SuppressWarnings("unchecked")
  private String renderAgentTask(Map<String, Object> result, String jaegerUrl) {
    Map<String, Object> agent = (Map<String, Object>) result.getOrDefault("agent", Map.of());
    Map<String, String> trace = (Map<String, String>) result.getOrDefault("trace", Map.of());
    Map<String, Object> database = (Map<String, Object>) result.getOrDefault("database", Map.of());
    Map<String, Object> workload = (Map<String, Object>) result.getOrDefault("workload", Map.of());
    Map<String, Object> bindValues = (Map<String, Object>) result.getOrDefault("bindValues", Map.of());
    Map<String, Object> sqlBridge = (Map<String, Object>) result.getOrDefault("sqlBridge", Map.of());
    Map<String, Object> sqlStats = (Map<String, Object>) result.getOrDefault("sqlStats", Map.of());
    Map<String, Object> sqlDetails = (Map<String, Object>) result.getOrDefault("sqlDetails", Map.of());
    Map<String, Object> sqlMonitor = (Map<String, Object>) result.getOrDefault("sqlMonitor", Map.of());
    Map<String, Object> dbmsXplan = (Map<String, Object>) result.getOrDefault("dbmsXplan", Map.of());

    String traceId = trace.getOrDefault("traceId", "");
    String jaegerTraceUrl = trimTrailingSlash(jaegerUrl) + "/trace/" + traceId;
    String xplan = String.join("\n", (List<String>) dbmsXplan.getOrDefault("lines", List.of()));
    String sqlText = String.valueOf(sqlDetails.getOrDefault("sqlText", ""));
    String bindSamples = formatRows((List<Map<String, Object>>) sqlDetails.getOrDefault("bindSamples", List.of()));
    String sqlMonitorPreview = String.valueOf(sqlMonitor.getOrDefault("reportPreview", "SQL Monitor report unavailable."));

    StringBuilder html = new StringBuilder();
    html.append("""
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Oracle DB OTel Agent Trace</title>
          <style>
            :root { color-scheme: light; --ink:#17202a; --muted:#5c6b7a; --line:#d8e0e8; --panel:#ffffff; --bg:#f4f7fa; --blue:#1f6fb2; --orange:#c95d13; }
            body { margin:0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color:var(--ink); background:var(--bg); }
            main { max-width: 1180px; margin: 0 auto; padding: 28px; }
            header { display:flex; justify-content:space-between; gap:18px; align-items:flex-start; margin-bottom:18px; }
            h1 { margin:0; font-size:28px; letter-spacing:0; }
            h2 { margin:0 0 10px; font-size:18px; }
            a.button { display:inline-block; color:#fff; background:var(--blue); padding:10px 14px; border-radius:6px; text-decoration:none; font-weight:700; }
            .sub { color:var(--muted); margin-top:5px; }
            .grid { display:grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap:16px; }
            .wide { grid-column: 1 / -1; }
            section { background:var(--panel); border:1px solid var(--line); border-radius:8px; padding:16px; box-shadow:0 10px 24px rgba(23,32,42,.06); }
            dl { display:grid; grid-template-columns: 180px minmax(0, 1fr); gap:8px 14px; margin:0; }
            dt { color:var(--muted); font-weight:700; }
            dd { margin:0; overflow-wrap:anywhere; }
            .pill { display:inline-block; padding:4px 8px; border-radius:999px; background:#eaf3fb; color:#174e7d; font-weight:700; font-size:12px; }
            .dbpill { background:#fff0e6; color:#8b3908; }
            pre { margin:0; padding:14px; background:#0f1720; color:#edf4fa; border-radius:6px; overflow:auto; line-height:1.35; font-size:13px; max-height:520px; }
            iframe { width:100%; height:760px; border:1px solid var(--line); border-radius:6px; background:#fff; }
            iframe.loading { display:flex; align-items:center; justify-content:center; }
            .flow { display:flex; flex-wrap:wrap; gap:8px; align-items:center; color:var(--muted); font-weight:700; }
            .flow span { padding:7px 9px; background:#eef3f7; border-radius:6px; }
            .flow b { color:var(--orange); }
            @media (max-width: 820px) { main { padding:16px; } header, .grid { display:block; } section { margin-bottom:14px; } dl { grid-template-columns:1fr; } }
          </style>
        </head>
        <body>
        <main>
        """);
    html.append("<header><div><h1>Agentic AI Database Trace</h1><div class=\"sub\">One visual trace, bridged to Oracle SQL Monitor and DBMS_XPLAN by <code>oracle.db.query.sql.id</code>.</div></div>");
    html.append("<a class=\"button\" href=\"").append(html(jaegerTraceUrl)).append("\" target=\"_blank\" rel=\"noopener\">Open Jaeger Trace</a></header>");

    html.append("<section class=\"wide\"><div class=\"flow\"><span>Agent task</span><span>Spring Boot</span><span>JDBC</span><span><b>Oracle DB Server span</b></span><span>SQL ID</span><span>SQL Monitor + DBMS_XPLAN</span></div></section>");

    html.append("<div class=\"grid\">");
    appendDetails(html, "Agent", agent);
    appendDetails(html, "Trace Bridge", Map.of(
        "traceId", traceId,
        "sqlId", sqlBridge.getOrDefault("sqlId", ""),
        "jaegerAttribute", sqlBridge.getOrDefault("jaegerAttribute", "oracle.db.query.sql.id"),
        "canonicalSource", sqlBridge.getOrDefault("canonicalSource", "DBMS_XPLAN and SQL Monitor")));
    appendDetails(html, "Database Session", database);
    appendDetails(html, "Agent Workload", workload);
    appendDetails(html, "Application Bind Values", bindValues);
    appendDetails(html, "SQL Runtime Stats", sqlStats);
    appendDetails(html, "SQL Monitor", Map.of(
        "available", sqlMonitor.getOrDefault("available", false),
        "type", sqlMonitor.getOrDefault("type", "TEXT"),
        "sqlId", sqlMonitor.getOrDefault("sqlId", "")));
    html.append("<section class=\"wide\"><h2>Full SQL Text <span class=\"sub\">from SQL_ID -> V$SQL.SQL_FULLTEXT</span></h2><pre>")
        .append(html(sqlText)).append("</pre></section>");
    html.append("<section class=\"wide\"><h2>Captured Bind Samples <span class=\"sub\">from SQL_ID -> V$SQL_BIND_CAPTURE</span></h2><p class=\"sub\">")
        .append(html(String.valueOf(sqlDetails.getOrDefault("bindCaptureNote", ""))))
        .append("</p><pre>").append(html(bindSamples)).append("</pre></section>");
    html.append("<section class=\"wide\"><h2>SQL Monitor Report Preview</h2><pre>")
        .append(html(sqlMonitorPreview)).append("</pre></section>");
    html.append("<section class=\"wide\"><h2>DBMS_XPLAN <span class=\"sub\">SQL execution plan: optimizer steps, estimated and actual rows, I/O, memory, and timing</span></h2><pre>")
        .append(html(xplan)).append("</pre></section>");
    html.append("<section class=\"wide\"><h2>Jaeger Trace</h2><p class=\"sub\">The embedded trace loads after a short delay so the completed Spring Boot, JDBC, and database spans have time to flush. If your browser blocks embedding, use the Open Jaeger Trace button above.</p><iframe id=\"jaeger-frame\" title=\"Jaeger trace\"></iframe></section>");
    html.append("</div><script>setTimeout(function(){document.getElementById('jaeger-frame').src='")
        .append(html(jaegerTraceUrl)).append("';}, 3500);</script></main></body></html>");
    return html.toString();
  }

  private void appendDetails(StringBuilder html, String title, Map<String, ?> values) {
    html.append("<section><h2>").append(html(title)).append("</h2><dl>");
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      html.append("<dt>").append(html(entry.getKey())).append("</dt><dd>")
          .append(html(String.valueOf(entry.getValue()))).append("</dd>");
    }
    html.append("</dl></section>");
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private String formatRows(List<Map<String, Object>> rows) {
    if (rows.isEmpty()) {
      return "No bind samples captured for this SQL. Oracle samples bind values opportunistically, so use the Application Bind Values panel above for the exact values used by this request.";
    }
    StringBuilder text = new StringBuilder();
    for (Map<String, Object> row : rows) {
      if (!text.isEmpty()) {
        text.append("\n");
      }
      text.append(row);
    }
    return text.toString();
  }

  private String html(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
