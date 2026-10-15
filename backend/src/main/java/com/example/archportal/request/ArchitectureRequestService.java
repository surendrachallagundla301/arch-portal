package com.example.archportal.request;

import com.example.archportal.cmdb.CmdbApplication;
import com.example.archportal.cmdb.CmdbClient;
import com.example.archportal.request.RequestDtos.CreateRequest;
import com.example.archportal.request.RequestDtos.DecisionRequest;
import com.example.archportal.request.RequestDtos.RequestResponse;
import com.example.archportal.web.NotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchitectureRequestService {

    private final ArchitectureRequestRepository repository;
    private final CmdbClient cmdbClient;
    private final Clock clock;

    public ArchitectureRequestService(ArchitectureRequestRepository repository, CmdbClient cmdbClient, Clock clock) {
        this.repository = repository;
        this.cmdbClient = cmdbClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> search(String query, RequestStatus status) {
        return repository.search(query, status).stream().map(RequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RequestResponse get(Long id) {
        return RequestResponse.from(load(id));
    }

    /**
     * Not transactional on purpose: the CMDB lookup (with retries) happens before we open a
     * transaction, so a slow CMDB never holds a database connection.
     */
    public RequestResponse create(CreateRequest body, String submittedBy) {
        String owner = cmdbClient.findApplication(body.applicationName())
                .map(CmdbApplication::owner)
                .orElse(null);
        ArchitectureRequest saved = repository.save(new ArchitectureRequest(
                body.title().trim(), body.applicationName().trim(), owner,
                body.description().trim(), submittedBy, clock.instant()));
        return RequestResponse.from(saved);
    }

    @Transactional
    public RequestResponse decide(Long id, DecisionRequest body, String reviewer) {
        ArchitectureRequest request = load(id);
        List<String> items = body.actionItems() == null ? List.of() : body.actionItems();
        request.recordDecision(body.outcome(), body.notes().trim(), items, reviewer, clock.instant());
        return RequestResponse.from(repository.saveAndFlush(request));
    }

    private ArchitectureRequest load(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Architecture request " + id + " was not found."));
    }
}
