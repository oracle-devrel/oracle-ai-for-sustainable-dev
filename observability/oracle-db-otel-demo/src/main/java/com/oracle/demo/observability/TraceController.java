package com.oracle.demo.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trace")
public class TraceController {

  private final JdbcTemplate jdbcTemplate;
  private final ObservationRegistry observationRegistry;
  private final Tracer tracer;

  public TraceController(JdbcTemplate jdbcTemplate, ObservationRegistry observationRegistry, Tracer tracer) {
    this.jdbcTemplate = jdbcTemplate;
    this.observationRegistry = observationRegistry;
    this.tracer = tracer;
  }

  @GetMapping("/roundtrip")
  public Map<String, Object> roundtrip() {
    return Observation
        .createNotStarted("oracle.demo.database-roundtrip", observationRegistry)
        .lowCardinalityKeyValue("db.system", "oracle")
        .observe(this::queryDatabase);
  }

  private Map<String, Object> queryDatabase() {
    Map<String, Object> databaseContext = jdbcTemplate.queryForMap("""
        select
          sys_context('USERENV', 'CURRENT_SCHEMA') as schema_name,
          sys_context('USERENV', 'SERVICE_NAME') as service_name,
          sys_context('USERENV', 'DB_NAME') as database_name
        from dual
        """);

    Number tableCount = jdbcTemplate.queryForObject("select count(*) from user_tables", Number.class);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("trace", currentTrace());
    response.put("database", databaseContext);
    response.put("userTableCount", tableCount == null ? 0 : tableCount.longValue());
    response.put("message", "Check the trace backend for the HTTP span, business observation, JDBC roundtrips, and database server spans.");
    return response;
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
}
