package com.jerokysys.invoices.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jerokysys.commons.common.Response;

/**
 * Atrapa las excepciones de @PreAuthorize, que ocurren dentro del MVC.
 * Sin este advice, el handler generico de commons las convertiria en 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Response.error("FORBIDDEN",
                        "No tiene permisos para acceder a este recurso",
                        HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Response.error("UNAUTHORIZED",
                        "Autenticación requerida",
                        HttpStatus.UNAUTHORIZED.value()));
    }
}