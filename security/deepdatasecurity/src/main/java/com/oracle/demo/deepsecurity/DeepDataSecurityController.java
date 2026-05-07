package com.oracle.demo.deepsecurity;

import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deepsec")
public class DeepDataSecurityController {

    private final DeepDataSecurityService deepDataSecurityService;

    DeepDataSecurityController(DeepDataSecurityService deepDataSecurityService) {
        this.deepDataSecurityService = deepDataSecurityService;
    }

    @GetMapping("/query")
    public ResponseEntity<?> query(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String endUserToken = extractBearerToken(authorization);
        if (endUserToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing Authorization: Bearer token"));
        }

        try {
            List<String> rows = deepDataSecurityService.queryAsEndUser(endUserToken);
            return ResponseEntity.ok(new QueryResponse(rows));
        } catch (SQLException | RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isBlank() ? null : token;
    }

    record QueryResponse(List<String> rows) {
    }

    record HealthResponse(String status) {
    }

    record ErrorResponse(String error) {
    }
}
