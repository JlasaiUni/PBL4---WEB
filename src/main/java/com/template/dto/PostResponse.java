package com.template.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/** DTO de lectura para exponer datos de un post. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

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
