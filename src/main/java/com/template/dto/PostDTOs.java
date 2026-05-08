package com.template.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// ── Post DTOs ─────────────────────────────────────────────────

public class PostDTOs {

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePostRequest {

        @NotBlank(message = "El título es obligatorio")
        @Size(min = 3, max = 200, message = "El título debe tener entre 3 y 200 caracteres")
        private String title;

        @NotBlank(message = "El contenido es obligatorio")
        private String content;

        private boolean published;

        private Set<String> tags = new HashSet<>();
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PostResponse {
        private Long id;
        private String title;
        private String content;
        private boolean published;
        private String authorUsername;
        private String authorFullName;
        private Set<String> tags;
        private int commentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
