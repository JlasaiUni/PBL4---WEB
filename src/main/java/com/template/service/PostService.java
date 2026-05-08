package com.template.service;

import com.template.dto.PostDTOs;
import com.template.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostService {
    Post create(PostDTOs.CreatePostRequest request, String authorUsername);
    Optional<Post> findById(Long id);
    Page<PostDTOs.PostResponse> findPublished(Pageable pageable);
    Page<PostDTOs.PostResponse> search(String query, Pageable pageable);
    Post update(Long id, PostDTOs.CreatePostRequest request, String currentUsername);
    void delete(Long id, String currentUsername);
    PostDTOs.PostResponse toResponse(Post post);
}