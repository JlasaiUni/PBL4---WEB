package com.template.controller;

import com.template.dto.CreatePostRequest;
import com.template.dto.PostResponse;
import com.template.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * API REST para consumo desde JavaScript (AJAX / Fetch).
 * Devuelve JSON. Complementa las vistas MVC Thymeleaf.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "CRUD de posts publicos")
public class PostApiController {

    private final PostService postService;

    @GetMapping("/posts")
    @Operation(summary = "Listar posts publicados con paginacion")
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(postService.findPublished(pageable));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Obtener un post por ID")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return postService.findResponseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/posts")
    @Operation(summary = "Crear un nuevo post (requiere autenticacion)")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        var post = postService.create(request, currentUsername());
        return ResponseEntity
                .created(URI.create("/api/v1/posts/" + post.getId()))
                .body(postService.toResponse(post));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "Actualizar un post (autor o ADMIN)")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody CreatePostRequest request) {
        var updated = postService.update(id, request);
        return ResponseEntity.ok(postService.toResponse(updated));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "Eliminar un post (autor o ADMIN)")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/search")
    @Operation(summary = "Buscar posts por titulo")
    public ResponseEntity<Page<PostResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(
                postService.search(q, PageRequest.of(page, 10)));
    }

    // Helper
    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous";
    }
}
