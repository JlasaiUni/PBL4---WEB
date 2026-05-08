package com.template.controller;

import com.template.dto.AuthDTOs;
import com.template.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    // ── Login ─────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("errorMsg", "Usuario o contraseña incorrectos.");
        if (logout != null) model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        return "auth/login";
    }

    // ── Registro ──────────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new AuthDTOs.RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") AuthDTOs.RegisterRequest request,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("successMsg",
                    "¡Cuenta creada! Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }
}