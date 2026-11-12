package com.jerokysys.invoices.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerokysys.commons.common.Response;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Errores de seguridad producidos en la cadena de filtros (antes del MVC).
 * Nunca expone el mensaje de la excepcion ni trazas.
 */
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestSecurityErrorHandler.class);

    private final ObjectMapper objectMapper;

    public RestSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        boolean sentToken = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        log.warn("401 {} {} usuario={}", request.getMethod(), request.getRequestURI(), currentUser());

        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                sentToken ? "Token inválido o expirado" : "Autenticación requerida");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        log.warn("403 {} {} usuario={}", request.getMethod(), request.getRequestURI(), currentUser());

        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "No tiene permisos para acceder a este recurso");
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return "anonimo";
        }
        return authentication.getName();
    }

    private void write(HttpServletResponse response, HttpStatus status,
                       String code, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                Response.error(code, message, status.value()));
    }
}