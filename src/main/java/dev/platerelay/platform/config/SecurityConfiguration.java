package dev.platerelay.platform.config;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/donations")
                                .hasRole("DONOR")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/donations/*/cancel")
                                .hasRole("DONOR")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/recipients")
                                .hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/donations/*/claims")
                                .hasRole("RECIPIENT")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/claims/*/complete",
                                "/api/v1/claims/*/release").hasRole("RECIPIENT")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authorities);
        return converter;
    }

    private Collection<GrantedAuthority> authorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap) || !(realmMap.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                .toList();
    }
}
