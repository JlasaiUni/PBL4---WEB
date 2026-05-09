package com.template.service.impl;

import com.template.dto.CreatePostRequest;
import com.template.dto.PostResponse;
import com.template.entity.Post;
import com.template.entity.Tag;
import com.template.entity.User;
import com.template.event.PostPublishedEvent;
import com.template.exception.ResourceNotFoundException;
import com.template.exception.UnauthorizedException;
import com.template.repository.PostRepository;
import com.template.repository.TagRepository;
import com.template.repository.UserRepository;
import com.template.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Post create(CreatePostRequest request, String authorUsername) {
        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", authorUsername));

        Set<Tag> tags = resolveTags(request.getTags());

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .published(request.isPublished())
                .author(author)
                .tags(tags)
                .build();

        Post saved = postRepository.save(post);
        log.info("Post creado por {}: '{}'", authorUsername, saved.getTitle());

        if (saved.isPublished()) {
            eventPublisher.publishEvent(new PostPublishedEvent(this, saved));
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostResponse> findResponseById(Long id) {
        return postRepository.findById(id).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> findPublished(Pageable pageable) {
        return postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> search(String query, Pageable pageable) {
        return postRepository.searchByTitle(query, pageable).map(this::toResponse);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Post update(Long id, CreatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!isAdmin() && !post.getAuthor().getUsername().equals(currentUsername())) {
            throw new UnauthorizedException("No tienes permiso para editar este post");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPublished(request.isPublished());
        post.setTags(resolveTags(request.getTags()));

        return postRepository.save(post);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!isAdmin() && !post.getAuthor().getUsername().equals(currentUsername())) {
            throw new UnauthorizedException("No tienes permiso para eliminar este post");
        }

        postRepository.delete(post);
        log.info("Post eliminado: {} por {}", id, currentUsername());
    }

    @Override
    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .published(post.isPublished())
                .authorUsername(post.getAuthor().getUsername())
                .authorFullName(post.getAuthor().getFullName())
                .tags(post.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .commentCount(post.getComments().size())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // Helpers de seguridad
    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous";
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // Helpers de dominio
    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null) return new HashSet<>();
        return tagNames.stream()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(null, name, new HashSet<>()))))
                .collect(Collectors.toSet());
    }
}
