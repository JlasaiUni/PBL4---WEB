package com.template.dto;

import jakarta.validation.constraints.*;
import lombok.*;

// ── Auth DTOs ─────────────────────────────────────────────────

public class AuthDTOs {

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "El usuario o email es obligatorio")
        private String usernameOrEmail;

        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Email(message = "Email no válido")
        private String email;

        @NotBlank
        @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres")
        private String password;

        @NotBlank
        private String fullName;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private String username;
        private String email;
        private java.util.List<String> roles;
    }
}