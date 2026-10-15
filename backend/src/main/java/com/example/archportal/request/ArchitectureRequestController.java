package com.example.archportal.request;

import com.example.archportal.request.RequestDtos.CreateRequest;
import com.example.archportal.request.RequestDtos.DecisionRequest;
import com.example.archportal.request.RequestDtos.RequestResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class ArchitectureRequestController {

    private final ArchitectureRequestService service;

    public ArchitectureRequestController(ArchitectureRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequestResponse> search(@RequestParam(name = "q", required = false) String query,
                                        @RequestParam(required = false) RequestStatus status) {
        return service.search(query, status);
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequest body,
                                                  @AuthenticationPrincipal Jwt jwt) {
        RequestResponse created = service.create(body, username(jwt));
        return ResponseEntity.created(URI.create("/api/requests/" + created.id())).body(created);
    }

    @PostMapping("/{id}/decisions")
    @PreAuthorize("hasRole('ARB_REVIEWER')")
    public RequestResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest body,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.decide(id, body, username(jwt));
    }

    private static String username(Jwt jwt) {
        String name = jwt.getClaimAsString("preferred_username");
        return name != null ? name : jwt.getSubject();
    }
}
