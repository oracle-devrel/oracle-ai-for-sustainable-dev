package com.oracle.demo.deepsecurity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deepsec")
public class DeepDataSecurityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDataSecurityController.class);

    private final DeepDataSecurityService deepDataSecurityService;

    DeepDataSecurityController(DeepDataSecurityService deepDataSecurityService) {
        this.deepDataSecurityService = deepDataSecurityService;
    }

    // Provider version: Spring Security requires and validates the bearer token before
    // this method runs; ojdbc-provider-spring reads it through the JDBC provider SPI.
    @GetMapping("/query")
    public ResponseEntity<?> query() {
        try {
            List<String> rows = deepDataSecurityService.queryAsEndUser();
            return ResponseEntity.ok(new QueryResponse(rows));
        } catch (SQLException | RuntimeException e) {
            LOGGER.error("Deep Data Security query failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(errorMessages(e)));
        }
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoami() {
        try {
            String username = deepDataSecurityService.usernameAsEndUser();
            return ResponseEntity.ok(new WhoamiResponse(username));
        } catch (SQLException | RuntimeException e) {
            LOGGER.error("Deep Data Security whoami failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(errorMessages(e)));
        }
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @GetMapping("/browser-config")
    public BrowserConfigResponse browserConfig() {
        DeepDataSecurityProperties.Browser browser = deepDataSecurityService.browserConfig();
        return new BrowserConfigResponse(browser.getTenantId(), browser.getClientId(), browserScope(browser.getScope()));
    }

    @GetMapping("/policies")
    public PolicyDemoResponse policies() {
        return new PolicyDemoResponse(
                "Oracle Deep Data Security declarative SQL policy examples",
                "ojdbc-provider-spring attaches the end-user security context; Oracle AI Database enforces these data grants at SQL execution time.",
                List.of(
                        new PolicyExample(
                                "Row-level: employee can see only their own record",
                                "row",
                                """
                                CREATE OR REPLACE DATA GRANT hr.HRAPP_EMPLOYEES_ACCESS
                                  AS SELECT, UPDATE(phone_number, first_name)
                                  ON hr.employees
                                  WHERE upper(user_name) = upper(ORA_END_USER_CONTEXT.username)
                                  TO HRAPP_EMPLOYEES
                                """),
                        new PolicyExample(
                                "Column-level: manager can view direct reports except SSN",
                                "column",
                                """
                                CREATE OR REPLACE DATA GRANT hr.HRAPP_MANAGER_ACCESS
                                  AS SELECT (ALL COLUMNS EXCEPT ssn), UPDATE (salary, department_id, first_name)
                                  ON hr.employees
                                  WHERE manager_id = ORA_END_USER_CONTEXT.HR.EMP_CTX.ID
                                  TO HRAPP_MANAGERS
                                """),
                        new PolicyExample(
                                "Cell-level: employee can update phone only on their own row",
                                "cell",
                                """
                                CREATE OR REPLACE DATA GRANT hr.HRAPP_EMPLOYEES_ACCESS
                                  AS SELECT, UPDATE(phone_number)
                                  ON hr.employees
                                  WHERE upper(user_name) = upper(ORA_END_USER_CONTEXT.username)
                                  TO HRAPP_EMPLOYEES
                                """)),
                Map.of(
                        "queryEndpoint", "GET /deepsec/query",
                        "endUserToken", "Bearer access token validated by Spring Security resource-server support",
                        "databaseContext", "ojdbc-provider-spring supplies EndUserSecurityContext with a database-access token and the incoming end-user token"));
    }

    record QueryResponse(List<String> rows) {
    }

    record WhoamiResponse(String username) {
    }

    record HealthResponse(String status) {
    }

    record BrowserConfigResponse(String tenantId, String clientId, String scope) {
    }

    record PolicyDemoResponse(
            String title,
            String enforcementPoint,
            List<PolicyExample> examples,
            Map<String, String> appFlow) {
    }

    record PolicyExample(String title, String level, String sql) {
    }

    private static List<String> errorMessages(Throwable throwable) {
        List<String> messages = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            messages.add(current.getClass().getSimpleName() + (message == null ? "" : ": " + message));
            current = current.getCause();
        }
        return messages;
    }

    private static String browserScope(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return "";
        }
        for (String scope : scopes.split(",")) {
            String trimmed = scope.trim();
            if (trimmed.startsWith("api://")) {
                return trimmed;
            }
        }
        return scopes.trim();
    }

    record ErrorResponse(List<String> errors) {
    }
}
