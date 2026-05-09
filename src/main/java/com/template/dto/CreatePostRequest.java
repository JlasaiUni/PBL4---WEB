package com.template.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/** DTO para crear o actualizar un post. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(min = 3, max = 200, message = "El titulo debe tener entre 3 y 200 caracteres")
    private String title;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    private boolean published;

    private Set<String> tags = new HashSet<>();
}
