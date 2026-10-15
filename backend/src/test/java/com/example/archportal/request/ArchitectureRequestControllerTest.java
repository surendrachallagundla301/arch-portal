package com.example.archportal.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.archportal.request.RequestDtos.RequestResponse;
import com.example.archportal.security.SecurityConfig;
import com.example.archportal.web.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ArchitectureRequestController.class)
@Import(SecurityConfig.class)
class ArchitectureRequestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ArchitectureRequestService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final RequestResponse SAMPLE = new RequestResponse(
            7L, "Move billing to EKS", "billing-service", "Payments Team",
            "Containerize billing and run it on EKS.", RequestStatus.SUBMITTED, "alice",
            Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), List.of());

    private static RequestPostProcessor user(String name, String role) {
        return jwt().jwt(j -> j.claim("preferred_username", name))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void rejectsCallsWithoutAToken() throws Exception {
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsRequestsForAnAuthenticatedUser() throws Exception {
        when(service.search(isNull(), eq(RequestStatus.SUBMITTED))).thenReturn(List.of(SAMPLE));

        mvc.perform(get("/api/requests").param("status", "SUBMITTED").with(user("alice", "SUBMITTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Move billing to EKS"))
                .andExpect(jsonPath("$[0].applicationOwner").value("Payments Team"));
    }

    @Test
    void createsARequestAsTheSignedInUser() throws Exception {
        when(service.create(any(), eq("alice"))).thenReturn(SAMPLE);

        mvc.perform(post("/api/requests").with(user("alice", "SUBMITTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Move billing to EKS","applicationName":"billing-service",
                                 "description":"Containerize billing and run it on EKS."}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/requests/7"));
    }

    @Test
    void returnsAReadableProblemForInvalidInput() throws Exception {
        mvc.perform(post("/api/requests").with(user("alice", "SUBMITTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"applicationName\":\"billing\",\"description\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        verify(service, never()).create(any(), any());
    }

    @Test
    void returns404WhenTheRequestDoesNotExist() throws Exception {
        when(service.get(99L)).thenThrow(new NotFoundException("Architecture request 99 was not found."));

        mvc.perform(get("/api/requests/99").with(user("alice", "SUBMITTER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Architecture request 99 was not found."));
    }

    @Test
    void onlyReviewersCanRecordDecisions() throws Exception {
        String body = "{\"outcome\":\"APPROVED\",\"notes\":\"Looks good\",\"actionItems\":[]}";

        mvc.perform(post("/api/requests/7/decisions").with(user("alice", "SUBMITTER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        when(service.decide(eq(7L), any(), eq("bob"))).thenReturn(SAMPLE);
        mvc.perform(post("/api/requests/7/decisions").with(user("bob", "ARB_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
