package com.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vistas Thymeleaf de errores comunes.
 */
@Controller
@RequestMapping("/error")
public class ErrorViewController {

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }

    @GetMapping("/404")
    public String notFound() {
        return "error/404";
    }

    @GetMapping("/500")
    public String serverError() {
        return "error/500";
    }
}
