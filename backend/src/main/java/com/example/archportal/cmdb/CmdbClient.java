package com.example.archportal.cmdb;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks up applications in the CMDB. Transient failures (timeouts, 5xx) are retried with
 * exponential backoff; if the CMDB is still unavailable the portal degrades gracefully and the
 * request is saved without an owner rather than failing the user's submission.
 */
@Component
public class CmdbClient {

    private static final Logger log = LoggerFactory.getLogger(CmdbClient.class);

    private final RestClient restClient;
    private final boolean enabled;

    public CmdbClient(RestClient.Builder builder, CmdbProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.enabled = properties.enabled();
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttemptsExpression = "${portal.cmdb.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${portal.cmdb.backoff-ms:200}", multiplier = 2))
    public Optional<CmdbApplication> findApplication(String name) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            CmdbApplication app = restClient.get()
                    .uri("/applications/{name}", name)
                    .retrieve()
                    .body(CmdbApplication.class);
            return Optional.ofNullable(app);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }

    @Recover
    public Optional<CmdbApplication> recover(RestClientException e, String name) {
        log.warn("CMDB lookup for '{}' failed after retries: {}", name, e.getMessage());
        return Optional.empty();
    }
}
