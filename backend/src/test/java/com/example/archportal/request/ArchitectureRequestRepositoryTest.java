package com.example.archportal.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** Runs the Flyway migration against an in-memory database and checks the search filters. */
@DataJpaTest
class ArchitectureRequestRepositoryTest {

    @Autowired
    private ArchitectureRequestRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void seed() {
        Instant t = Instant.parse("2026-10-01T00:00:00Z");
        repository.save(new ArchitectureRequest("Move billing to EKS", "billing-service", null,
                "Containerize billing and run it on EKS.", "alice", t));
        ArchitectureRequest approved = new ArchitectureRequest("Adopt Kafka for claims", "claims-api", null,
                "Replace polling with Kafka events.", "alice", t.plusSeconds(60));
        approved.recordDecision(RequestStatus.APPROVED, "Approved", List.of("Add DLQ"), "bob", t.plusSeconds(120));
        repository.save(approved);
        // Write to the database and clear the session so the tests read real rows back.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void filtersByTextAcrossTitleAndApplication() {
        assertThat(repository.search("BILLING", null)).extracting(ArchitectureRequest::getTitle)
                .containsExactly("Move billing to EKS");
        assertThat(repository.search("claims", null)).hasSize(1);
    }

    @Test
    void filtersByStatusAndReturnsNewestFirst() {
        assertThat(repository.search(null, RequestStatus.APPROVED)).extracting(ArchitectureRequest::getTitle)
                .containsExactly("Adopt Kafka for claims");
        assertThat(repository.search("", null)).extracting(ArchitectureRequest::getTitle)
                .containsExactly("Adopt Kafka for claims", "Move billing to EKS");
    }

    @Test
    void persistsDecisionsWithOrderedActionItems() {
        ArchitectureRequest saved = repository.search("kafka", null).get(0);
        assertThat(saved.getDecisions()).singleElement()
                .satisfies(d -> assertThat(d.getActionItems()).containsExactly("Add DLQ"));
    }
}
