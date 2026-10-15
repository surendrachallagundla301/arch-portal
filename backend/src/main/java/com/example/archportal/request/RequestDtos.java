package com.example.archportal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request and response bodies for the architecture request API. */
public final class RequestDtos {

    private RequestDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(min = 5, max = 150) String title,
            @NotBlank @Size(max = 100) String applicationName,
            @NotBlank @Size(min = 20, max = 4000) String description) {
    }

    public record DecisionRequest(
            @NotNull RequestStatus outcome,
            @NotBlank @Size(max = 2000) String notes,
            @Size(max = 20) List<@NotBlank @Size(max = 500) String> actionItems) {
    }

    public record DecisionResponse(
            Long id, RequestStatus outcome, String notes, List<String> actionItems,
            String decidedBy, Instant decidedAt) {

        static DecisionResponse from(ReviewDecision d) {
            return new DecisionResponse(d.getId(), d.getOutcome(), d.getNotes(),
                    List.copyOf(d.getActionItems()), d.getDecidedBy(), d.getDecidedAt());
        }
    }

    public record RequestResponse(
            Long id, String title, String applicationName, String applicationOwner, String description,
            RequestStatus status, String submittedBy, Instant createdAt, Instant updatedAt,
            List<DecisionResponse> decisions) {

        static RequestResponse from(ArchitectureRequest r) {
            return new RequestResponse(r.getId(), r.getTitle(), r.getApplicationName(), r.getApplicationOwner(),
                    r.getDescription(), r.getStatus(), r.getSubmittedBy(), r.getCreatedAt(), r.getUpdatedAt(),
                    r.getDecisions().stream().map(DecisionResponse::from).toList());
        }
    }
}
