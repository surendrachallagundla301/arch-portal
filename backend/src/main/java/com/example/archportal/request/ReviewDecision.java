package com.example.archportal.request;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "review_decision")
public class ReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ArchitectureRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus outcome;

    @Column(nullable = false, length = 2000)
    private String notes;

    @ElementCollection
    @CollectionTable(name = "decision_action_item", joinColumns = @JoinColumn(name = "decision_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "item", nullable = false, length = 500)
    private List<String> actionItems = new ArrayList<>();

    @Column(name = "decided_by", nullable = false, length = 100)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected ReviewDecision() {
        // for JPA
    }

    ReviewDecision(ArchitectureRequest request, RequestStatus outcome, String notes,
                   List<String> actionItems, String decidedBy, Instant decidedAt) {
        this.request = request;
        this.outcome = outcome;
        this.notes = notes;
        this.actionItems = actionItems == null ? new ArrayList<>() : new ArrayList<>(actionItems);
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public Long getId() { return id; }
    public RequestStatus getOutcome() { return outcome; }
    public String getNotes() { return notes; }
    public List<String> getActionItems() { return Collections.unmodifiableList(actionItems); }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
