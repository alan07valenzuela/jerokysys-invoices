package com.jerokysys.invoices.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerokysys.invoices.security.RestSecurityErrorHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /**
     * Rutas publicas: documentacion y manejo interno de errores.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestSecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentacion publica
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Politicas por URL: lectura
                        .requestMatchers(HttpMethod.GET, "/invoices/**", "/invoice-details/**")
                        .hasAnyRole("ADMIN", "SECRETARY")

                        // Politicas por URL: escritura
                        .requestMatchers(HttpMethod.POST, "/invoices")
                        .hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.PUT, "/invoices/**")
                        .hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.PATCH, "/invoices/**")
                        .hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.DELETE, "/invoices/**")
                        .hasAnyRole("ADMIN", "SECRETARY")

                        // Lo no declarado queda cerrado
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    @Bean
    public RestSecurityErrorHandler restSecurityErrorHandler(ObjectMapper objectMapper) {
        return new RestSecurityErrorHandler(objectMapper);
    }

    /**
     * Valida firma y expiracion. El algoritmo se fija en HS256 en el servidor:
     * nunca se toma el "alg" del token (evita algorithm confusion).
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:}") String issuer) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(hmacKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        if (issuer != null && !issuer.isBlank()) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        }
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${jwt.claim.user}") String userClaim,
            @Value("${jwt.claim.role}") String roleClaim) {

        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(roleClaim);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName(userClaim);
        return converter;
    }

    /**
     * Acepta el secreto en Base64 o texto plano
     */
    private static SecretKey hmacKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is required");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (bytes.length * 8 < 256) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 256 bits for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}