package com.example.archportal.request;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "architecture_request")
public class ArchitectureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "application_name", nullable = false, length = 100)
    private String applicationName;

    @Column(name = "application_owner", length = 150)
    private String applicationOwner;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decidedAt ASC")
    private List<ReviewDecision> decisions = new ArrayList<>();

    protected ArchitectureRequest() {
        // for JPA
    }

    public ArchitectureRequest(String title, String applicationName, String applicationOwner,
                               String description, String submittedBy, Instant now) {
        this.title = title;
        this.applicationName = applicationName;
        this.applicationOwner = applicationOwner;
        this.description = description;
        this.submittedBy = submittedBy;
        this.status = RequestStatus.SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Records an Architecture Review Board decision and moves the request to that status. */
    public ReviewDecision recordDecision(RequestStatus outcome, String notes, List<String> actionItems,
                                         String decidedBy, Instant now) {
        if (outcome == RequestStatus.SUBMITTED) {
            throw new IllegalArgumentException("A decision must approve, reject, or request changes.");
        }
        if (status.isClosed()) {
            throw new IllegalStateException("Request " + id + " is already " + status + " and cannot be re-decided.");
        }
        ReviewDecision decision = new ReviewDecision(this, outcome, notes, actionItems, decidedBy, now);
        decisions.add(decision);
        this.status = outcome;
        this.updatedAt = now;
        return decision;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getApplicationOwner() { return applicationOwner; }
    public String getDescription() { return description; }
    public RequestStatus getStatus() { return status; }
    public String getSubmittedBy() { return submittedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ReviewDecision> getDecisions() { return Collections.unmodifiableList(decisions); }
}
