package com.oracle.demo.interactiveai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ApprovalService {
    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();
    private final Clock clock;

    public ApprovalService() {
        this(Clock.systemUTC());
    }

    ApprovalService(Clock clock) {
        this.clock = clock;
    }

    public String issue(
            String actor,
            List<TransferRecommendation> recommendations) {
        String id = UUID.randomUUID().toString();
        Map<String, TransferRecommendation> exactRecommendations = recommendations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TransferRecommendation::recommendationId,
                        Function.identity()));
        pending.put(
                id,
                new PendingApproval(
                        actor,
                        exactRecommendations,
                        clock.instant().plus(Duration.ofMinutes(10))));
        return id;
    }

    public TransferRecommendation consume(
            String approvalId,
            String recommendationId,
            String actor) {
        PendingApproval approval = pending.remove(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException(
                    "Approval is missing, expired, or already used");
        }
        if (clock.instant().isAfter(approval.expiresAt())) {
            throw new IllegalArgumentException("Approval has expired");
        }
        if (!approval.actor().equals(actor)) {
            throw new IllegalArgumentException("Approval actor does not match");
        }
        TransferRecommendation recommendation =
                approval.recommendations().get(recommendationId);
        if (recommendation == null) {
            throw new IllegalArgumentException(
                    "Transfer recommendation was not part of the approved result set");
        }
        return recommendation;
    }

    public void reject(String approvalId, String actor) {
        PendingApproval approval = pending.remove(approvalId);
        if (approval == null || !approval.actor().equals(actor)) {
            throw new IllegalArgumentException(
                    "Approval is missing or actor does not match");
        }
    }

    public record PendingApproval(
            String actor,
            Map<String, TransferRecommendation> recommendations,
            Instant expiresAt) {
    }
}
