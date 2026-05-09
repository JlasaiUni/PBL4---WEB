package com.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** DTO para la peticion de login (JWT y formulario). */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "El usuario o email es obligatorio")
    private String usernameOrEmail;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
