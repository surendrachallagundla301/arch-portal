package com.example.archportal.cmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(CmdbClientTest.Config.class)
@TestPropertySource(properties = {"portal.cmdb.max-attempts=3", "portal.cmdb.backoff-ms=1"})
class CmdbClientTest {

    private static final String URL = "http://cmdb.test/applications/billing-service";

    @Configuration
    @EnableRetry
    static class Config {
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        MockRestServiceServer server(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean
        CmdbClient cmdbClient(RestClient.Builder builder, MockRestServiceServer server) {
            return new CmdbClient(builder, new CmdbProperties(true, "http://cmdb.test"));
        }
    }

    @Autowired
    private CmdbClient client;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void reset() {
        server.reset();
    }

    @Test
    void retriesTransientFailuresThenReturnsTheApplication() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"name\":\"billing-service\",\"owner\":\"Payments Team\",\"criticality\":\"HIGH\"}",
                MediaType.APPLICATION_JSON));

        assertThat(client.findApplication("billing-service"))
                .hasValueSatisfying(app -> assertThat(app.owner()).isEqualTo("Payments Team"));
        server.verify();
    }

    @Test
    void doesNotRetryWhenTheApplicationIsUnknown() {
        server.expect(times(1), requestTo(URL)).andRespond(withResourceNotFound());

        assertThat(client.findApplication("billing-service")).isEmpty();
        server.verify();
    }

    @Test
    void degradesGracefullyAfterTheLastAttempt() {
        server.expect(times(3), requestTo(URL)).andRespond(withServerError());

        assertThat(client.findApplication("billing-service")).isEmpty();
        server.verify();
    }
}
