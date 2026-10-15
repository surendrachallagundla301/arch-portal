package com.example.archportal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.archportal.cmdb.CmdbApplication;
import com.example.archportal.cmdb.CmdbClient;
import com.example.archportal.request.RequestDtos.CreateRequest;
import com.example.archportal.request.RequestDtos.DecisionRequest;
import com.example.archportal.request.RequestDtos.RequestResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchitectureRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock
    private ArchitectureRequestRepository repository;

    @Mock
    private CmdbClient cmdbClient;

    private ArchitectureRequestService service;

    @BeforeEach
    void setUp() {
        service = new ArchitectureRequestService(repository, cmdbClient, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createStoresTheOwnerFoundInTheCmdb() {
        when(cmdbClient.findApplication("billing-service"))
                .thenReturn(Optional.of(new CmdbApplication("billing-service", "Payments Team", "HIGH")));
        when(repository.save(any(ArchitectureRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse created = service.create(
                new CreateRequest("Move billing to EKS", "billing-service", "Containerize billing and run it on EKS."),
                "alice");

        assertThat(created.applicationOwner()).isEqualTo("Payments Team");
        assertThat(created.status()).isEqualTo(RequestStatus.SUBMITTED);
        assertThat(created.submittedBy()).isEqualTo("alice");
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void createStillSucceedsWhenTheCmdbHasNoRecord() {
        when(cmdbClient.findApplication("new-app")).thenReturn(Optional.empty());
        when(repository.save(any(ArchitectureRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse created = service.create(
                new CreateRequest("Introduce new-app", "new-app", "A brand new service not yet in the CMDB."),
                "alice");

        assertThat(created.applicationOwner()).isNull();
    }

    @Test
    void decideRecordsTheDecisionAndUpdatesStatus() {
        ArchitectureRequest request = new ArchitectureRequest("Move billing to EKS", "billing-service",
                "Payments Team", "Containerize billing and run it on EKS.", "alice", NOW);
        when(repository.findById(7L)).thenReturn(Optional.of(request));
        when(repository.saveAndFlush(request)).thenReturn(request);

        RequestResponse result = service.decide(7L,
                new DecisionRequest(RequestStatus.NEEDS_CHANGES, "Add a rollback plan", List.of("Document rollback")),
                "bob");

        assertThat(result.status()).isEqualTo(RequestStatus.NEEDS_CHANGES);
        assertThat(result.decisions()).singleElement().satisfies(d -> {
            assertThat(d.decidedBy()).isEqualTo("bob");
            assertThat(d.actionItems()).containsExactly("Document rollback");
        });
    }

    @Test
    void closedRequestsCannotBeDecidedAgain() {
        ArchitectureRequest request = new ArchitectureRequest("Move billing to EKS", "billing-service",
                null, "Containerize billing and run it on EKS.", "alice", NOW);
        request.recordDecision(RequestStatus.APPROVED, "Approved", List.of(), "bob", NOW);
        when(repository.findById(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.decide(7L,
                new DecisionRequest(RequestStatus.REJECTED, "Changed my mind", null), "carol"))
                .isInstanceOf(IllegalStateException.class);
    }
}
