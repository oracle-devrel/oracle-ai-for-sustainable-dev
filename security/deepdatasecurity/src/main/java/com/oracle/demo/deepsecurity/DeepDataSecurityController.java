package com.oracle.demo.deepsecurity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
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

    @GetMapping("/query")
    public ResponseEntity<?> query(
            @RegisteredOAuth2AuthorizedClient("entra") OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(List.of("Sign in with Microsoft Entra ID before calling /deepsec/query")));
        }
        String endUserToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            List<String> rows = deepDataSecurityService.queryAsEndUser(endUserToken);
            return ResponseEntity.ok(new QueryResponse(rows));
        } catch (SQLException | RuntimeException e) {
            LOGGER.error("Deep Data Security query failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(errorMessages(e)));
        }
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoami(
            @RegisteredOAuth2AuthorizedClient("entra") OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(List.of("Sign in with Microsoft Entra ID before calling /deepsec/whoami")));
        }
        String endUserToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            String username = deepDataSecurityService.usernameAsEndUser(endUserToken);
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

    @GetMapping("/policies")
    public PolicyDemoResponse policies() {
        return new PolicyDemoResponse(
                "Oracle Deep Data Security declarative SQL policy examples",
                "The application attaches the end-user security context; Oracle AI Database enforces these data grants at SQL execution time.",
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
                        "endUserToken", "Spring Security OAuth2 login access token from Microsoft Entra ID",
                        "databaseContext", "EndUserSecurityContext with database-access token and end-user token; Entra-mapped roles activate from the token"));
    }

    record QueryResponse(List<String> rows) {
    }

    record WhoamiResponse(String username) {
    }

    record HealthResponse(String status) {
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

    record ErrorResponse(List<String> errors) {
    }
}
