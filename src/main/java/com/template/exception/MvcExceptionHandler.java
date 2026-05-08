package com.template.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handler de excepciones para controladores MVC (Thymeleaf).
 * Devuelve la vista correspondiente en lugar de JSON.
 *
 * Sólo aplica a controladores anotados con @Controller (no @RestController),
 * por lo que la API REST sigue devolviendo JSON gracias a
 * {@link GlobalExceptionHandler}.
 */
@ControllerAdvice(annotations = Controller.class)
@Slf4j
public class MvcExceptionHandler {

    // 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex,
                                 Model model,
                                 HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    // 400
    @ExceptionHandler(BadRequestException.class)
    public String handleBadRequest(BadRequestException ex,
                                   Model model,
                                   HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("message", ex.getMessage());
        return "error/500"; // No hay vista 400 dedicada; reutilizamos 500.
    }

    // 401
    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException ex,
                                     Model model,
                                     HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     Model model,
                                     HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("message", "Acceso denegado");
        return "error/403";
    }

    // 500 genérico
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex,
                                Model model,
                                HttpServletResponse response) {
        log.error("Error inesperado en ruta MVC: {}", ex.getMessage(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("message", "Error interno del servidor");
        return "error/500";
    }
}
