package com.oracle.demo.deepsecurity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/app.js",
                                "/styles.css",
                                "/deepsec/browser-config",
                                "/deepsec/health",
                                "/deepsec/policies").permitAll()
                        .anyRequest().authenticated())
                .oauth2Client(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(jwtAuthenticationManagerResolver));
        return http.build();
    }

    @Bean
    AuthenticationManagerResolver<HttpServletRequest> jwtAuthenticationManagerResolver(
            DeepDataSecurityProperties properties) {
        String[] trustedIssuers = Arrays.stream(properties.getJwt().getTrustedIssuers().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        if (trustedIssuers.length == 0) {
            throw new IllegalStateException(
                    "Configure at least one trusted JWT issuer with deepsec.jwt.trusted-issuers.");
        }
        return JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(trustedIssuers);
    }
}
