package com.template.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** DTO para el registro de nuevos usuarios. */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email(message = "Email no valido")
    private String email;

    @NotBlank
    @Size(min = 6, max = 40, message = "La contrasena debe tener entre 6 y 40 caracteres")
    private String password;

    @NotBlank
    private String fullName;
}
