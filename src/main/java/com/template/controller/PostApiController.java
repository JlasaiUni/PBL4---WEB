package com.template.controller;

import com.template.dto.PostDTOs;
import com.template.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * API REST para consumo desde JavaScript (AJAX / Fetch).
 * Devuelve JSON. Complementa las vistas MVC Thymeleaf.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    @GetMapping("/posts")
    public ResponseEntity<Page<PostDTOs.PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(postService.findPublished(pageable));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostDTOs.PostResponse> getPost(@PathVariable Long id) {
        return postService.findById(id)
                .map(p -> ResponseEntity.ok(postService.toResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTOs.PostResponse> createPost(
            @Valid @RequestBody PostDTOs.CreatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        var post = postService.create(request, userDetails.getUsername());
        return ResponseEntity
                .created(URI.create("/api/v1/posts/" + post.getId()))
                .body(postService.toResponse(post));
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTOs.PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostDTOs.CreatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        var updated = postService.update(id, request, userDetails.getUsername());
        return ResponseEntity.ok(postService.toResponse(updated));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        postService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/search")
    public ResponseEntity<Page<PostDTOs.PostResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(
                postService.search(q, PageRequest.of(page, 10)));
    }
}