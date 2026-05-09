package com.template.service;

import com.template.dto.CreatePostRequest;
import com.template.dto.PostResponse;
import com.template.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PostService {
    Post create(CreatePostRequest request, String authorUsername);
    Optional<Post> findById(Long id);
    Optional<PostResponse> findResponseById(Long id);
    Page<PostResponse> findPublished(Pageable pageable);
    Page<PostResponse> search(String query, Pageable pageable);
    Post update(Long id, CreatePostRequest request);
    void delete(Long id);
    PostResponse toResponse(Post post);
}
