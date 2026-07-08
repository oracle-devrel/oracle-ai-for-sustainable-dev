package com.oracle.demo.lakehouse;

import java.sql.SQLException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LakehouseController {

    private final LakehouseQueryService service;

    LakehouseController(LakehouseQueryService service) {
        this.service = service;
    }

    @GetMapping("/")
    Map<String, Object> home() {
        return Map.of(
                "name", "Oracle AI Database Lakehouse + Iceberg JDBC demo",
                "endpoints", Map.of(
                        "database", "/api/database",
                        "configuredTableSummary", "/api/lakehouse/summary",
                        "sampleRows", "/api/lakehouse/sample?tableName=YOUR_SCHEMA.YOUR_ICEBERG_TABLE&limit=25",
                        "columns", "/api/lakehouse/columns?tableName=YOUR_SCHEMA.YOUR_ICEBERG_TABLE"));
    }

    @GetMapping("/api/database")
    Map<String, Object> database() throws SQLException {
        return service.databaseInfo();
    }

    @GetMapping("/api/lakehouse/summary")
    Map<String, Object> summary() throws SQLException {
        return service.configuredTableSummary();
    }

    @GetMapping("/api/lakehouse/sample")
    Object sample(
            @RequestParam String tableName,
            @RequestParam(defaultValue = "25") int limit)
            throws SQLException {
        return service.sampleRows(tableName, limit);
    }

    @GetMapping("/api/lakehouse/columns")
    Object columns(@RequestParam String tableName) throws SQLException {
        return service.columns(tableName);
    }

    @GetMapping("/api/lakehouse/help")
    Map<String, Object> help() {
        return Map.of(
                "purpose", "Query Oracle SQL objects backed by Oracle lakehouse and Iceberg table support through normal JDBC.",
                "configure", "Set DB_URL, DB_USERNAME, DB_PASSWORD, and ICEBERG_TABLE_NAME.",
                "note", "The Iceberg table is created and managed by Oracle Database/lakehouse SQL features; the Spring app uses Oracle JDBC to query it like a normal SQL table or view.");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
        IllegalArgumentException.class,
        SQLException.class
    })
    ResponseEntity<Map<String, Object>> handle(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", exception.getClass().getSimpleName(),
                "message", exception.getMessage()));
    }
}
