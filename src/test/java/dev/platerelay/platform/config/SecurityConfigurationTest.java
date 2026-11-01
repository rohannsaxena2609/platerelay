package dev.platerelay.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigurationTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void mapsRealmRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("DONOR", "recipient")))
                .build();

        AbstractAuthenticationToken authentication = configuration.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_DONOR", "ROLE_RECIPIENT");
    }

    @Test
    void givesNoRolesWhenRealmRoleClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("test-user")
                .claim("scope", "profile").build();
        assertThat(configuration.jwtAuthenticationConverter().convert(jwt).getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority))
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }
}
