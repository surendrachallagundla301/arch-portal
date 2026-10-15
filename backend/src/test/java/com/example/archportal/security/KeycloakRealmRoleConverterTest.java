package com.example.archportal.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void mapsRealmRolesToSpringRoles() {
        Jwt jwt = token().claim("realm_access", Map.of("roles", List.of("SUBMITTER", "ARB_REVIEWER"))).build();

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SUBMITTER", "ROLE_ARB_REVIEWER");
    }

    @Test
    void returnsNoRolesWhenTheClaimIsMissing() {
        Jwt jwt = token().claim("sub", "abc").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
