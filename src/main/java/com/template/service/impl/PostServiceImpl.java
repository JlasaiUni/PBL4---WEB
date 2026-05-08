package com.template.service.impl;

import com.template.dto.PostDTOs;
import com.template.entity.Post;
import com.template.entity.Tag;
import com.template.entity.User;
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
    public Post create(PostDTOs.CreatePostRequest request, String authorUsername) {
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

        // Publicar evento de aplicación (desacoplado – p.ej. para notificaciones WS)
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
    public Page<PostDTOs.PostResponse> findPublished(Pageable pageable) {
        return postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTOs.PostResponse> search(String query, Pageable pageable) {
        return postRepository.searchByTitle(query, pageable).map(this::toResponse);
    }

    @Override
    public Post update(Long id, PostDTOs.CreatePostRequest request, String currentUsername) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        // Sólo el autor o ADMIN puede editar
        if (!post.getAuthor().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("No tienes permiso para editar este post");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPublished(request.isPublished());
        post.setTags(resolveTags(request.getTags()));

        return postRepository.save(post);
    }

    @Override
    public void delete(Long id, String currentUsername) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        if (!post.getAuthor().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("No tienes permiso para eliminar este post");
        }

        postRepository.delete(post);
        log.info("Post eliminado: {} por {}", id, currentUsername);
    }

    @Override
    public PostDTOs.PostResponse toResponse(Post post) {
        return PostDTOs.PostResponse.builder()
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

    // ── Helpers ──────────────────────────────────────────────
    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null) return new HashSet<>();
        return tagNames.stream()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(null, name, new HashSet<>()))))
                .collect(Collectors.toSet());
    }

    // ── Evento interno de aplicación ─────────────────────────
    public static class PostPublishedEvent extends org.springframework.context.ApplicationEvent {
        private final Post post;
        public PostPublishedEvent(Object source, Post post) {
            super(source);
            this.post = post;
        }
        public Post getPost() { return post; }
    }
}