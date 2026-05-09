package com.template.service;

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
import com.template.service.impl.PostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del PostServiceImpl con Mockito.
 * No levanta contexto Spring ni BD.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl")
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private PostServiceImpl postService;

    private User author;
    private Post savedPost;
    private CreatePostRequest createRequest;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L)
                .username("jon")
                .email("jon@test.com")
                .fullName("Jon Test")
                .password("hashed")
                .build();

        savedPost = Post.builder()
                .id(10L)
                .title("Titulo de prueba")
                .content("Contenido de prueba")
                .published(true)
                .author(author)
                .tags(new HashSet<>())
                .comments(new HashSet<>())
                .build();

        createRequest = new CreatePostRequest(
                "Titulo de prueba", "Contenido de prueba", true, Set.of("java"));

        setSecurityContext("jon", "ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String username, String... roles) {
        var authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── create() ─────────────────────────────────────────────

    @Test
    @DisplayName("create() guarda el post y lanza PostPublishedEvent si published=true")
    void create_guardaPostYPublicaEvento_cuandoPublishedEsTrue() {
        when(userRepository.findByUsername("jon")).thenReturn(Optional.of(author));
        when(tagRepository.findByName("java")).thenReturn(Optional.of(new Tag(1L, "java", new HashSet<>())));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        Post result = postService.create(createRequest, "jon");

        assertThat(result.getTitle()).isEqualTo("Titulo de prueba");
        assertThat(result.isPublished()).isTrue();
        verify(postRepository).save(any(Post.class));
        verify(eventPublisher).publishEvent(any(PostPublishedEvent.class));
    }

    @Test
    @DisplayName("create() NO lanza PostPublishedEvent si published=false")
    void create_noPublicaEvento_cuandoPublishedEsFalse() {
        CreatePostRequest draftRequest =
                new CreatePostRequest("Borrador", "Contenido", false, Set.of());

        Post draft = Post.builder()
                .id(11L).title("Borrador").content("Contenido")
                .published(false).author(author)
                .tags(new HashSet<>()).comments(new HashSet<>()).build();

        when(userRepository.findByUsername("jon")).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(draft);

        postService.create(draftRequest, "jon");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("create() lanza ResourceNotFoundException si el usuario no existe")
    void create_lanzaExcepcion_cuandoUsuarioNoExiste() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.create(createRequest, "fantasma"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    // ── findById() / findResponseById() ──────────────────────

    @Test
    @DisplayName("findById() devuelve Optional con el post cuando existe")
    void findById_devuelvePost_cuandoExiste() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        Optional<Post> result = postService.findById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("findById() devuelve Optional vacio cuando no existe")
    void findById_devuelveVacio_cuandoNoExiste() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(postService.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findResponseById() mapea correctamente a PostResponse")
    void findResponseById_mapeaCorrectamente() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        Optional<PostResponse> result = postService.findResponseById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Titulo de prueba");
        assertThat(result.get().getAuthorUsername()).isEqualTo("jon");
    }

    // ── findPublished() / search() ────────────────────────────

    @Test
    @DisplayName("findPublished() devuelve pagina de PostResponse")
    void findPublished_devuelvePagina() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(savedPost), pageable, 1);
        when(postRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<PostResponse> result = postService.findPublished(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Titulo de prueba");
    }

    @Test
    @DisplayName("search() delega en el repositorio con la query dada")
    void search_delegaEnRepositorio() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(savedPost));
        when(postRepository.searchByTitle("prueba", pageable)).thenReturn(page);

        Page<PostResponse> result = postService.search("prueba", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(postRepository).searchByTitle("prueba", pageable);
    }

    // ── update() ─────────────────────────────────────────────

    @Test
    @DisplayName("update() actualiza los campos y guarda cuando es el autor")
    void update_actualizaPost_cuandoEsElAutor() {
        CreatePostRequest updateReq =
                new CreatePostRequest("Nuevo titulo", "Nuevo contenido", true, Set.of());

        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.update(10L, updateReq);

        assertThat(result.getTitle()).isEqualTo("Nuevo titulo");
        assertThat(result.getContent()).isEqualTo("Nuevo contenido");
        verify(postRepository).save(savedPost);
    }

    @Test
    @DisplayName("update() permite al ADMIN editar el post de otro usuario")
    void update_permiteAdmin_editarPostAjeno() {
        setSecurityContext("admin", "ROLE_ADMIN");
        CreatePostRequest updateReq =
                new CreatePostRequest("Titulo admin", "Contenido", true, Set.of());

        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.update(10L, updateReq);

        assertThat(result.getTitle()).isEqualTo("Titulo admin");
        verify(postRepository).save(savedPost);
    }

    @Test
    @DisplayName("update() lanza ResourceNotFoundException si el post no existe")
    void update_lanzaExcepcion_cuandoPostNoExiste() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(99L, createRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update() lanza UnauthorizedException si no es el autor ni admin")
    void update_lanzaUnauthorized_cuandoNoEsElAutorNiAdmin() {
        setSecurityContext("intruso", "ROLE_USER");
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        assertThatThrownBy(() -> postService.update(10L, createRequest))
                .isInstanceOf(UnauthorizedException.class);

        verify(postRepository, never()).save(any());
    }

    // ── delete() ─────────────────────────────────────────────

    @Test
    @DisplayName("delete() elimina el post si el usuario es el autor")
    void delete_eliminaPost_cuandoEsElAutor() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        postService.delete(10L);

        verify(postRepository).delete(savedPost);
    }

    @Test
    @DisplayName("delete() permite al ADMIN borrar el post de otro usuario")
    void delete_permiteAdmin_borrarPostAjeno() {
        setSecurityContext("admin", "ROLE_ADMIN");
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        postService.delete(10L);

        verify(postRepository).delete(savedPost);
    }

    @Test
    @DisplayName("delete() lanza ResourceNotFoundException si el post no existe")
    void delete_lanzaExcepcion_cuandoPostNoExiste() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete() lanza UnauthorizedException si no es el autor ni admin")
    void delete_lanzaUnauthorized_cuandoNoEsElAutorNiAdmin() {
        setSecurityContext("intruso", "ROLE_USER");
        when(postRepository.findById(10L)).thenReturn(Optional.of(savedPost));

        assertThatThrownBy(() -> postService.delete(10L))
                .isInstanceOf(UnauthorizedException.class);

        verify(postRepository, never()).delete(any());
    }

    // ── toResponse() ─────────────────────────────────────────

    @Test
    @DisplayName("toResponse() mapea todos los campos del Post a PostResponse")
    void toResponse_mapeaTodosLosCampos() {
        Tag tag = new Tag(1L, "java", new HashSet<>());
        savedPost.setTags(Set.of(tag));
        savedPost.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        PostResponse response = postService.toResponse(savedPost);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Titulo de prueba");
        assertThat(response.getContent()).isEqualTo("Contenido de prueba");
        assertThat(response.isPublished()).isTrue();
        assertThat(response.getAuthorUsername()).isEqualTo("jon");
        assertThat(response.getAuthorFullName()).isEqualTo("Jon Test");
        assertThat(response.getTags()).containsExactly("java");
        assertThat(response.getCommentCount()).isZero();
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
    }
}
