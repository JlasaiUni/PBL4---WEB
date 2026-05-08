package com.template.repository;

import com.template.entity.Post;
import com.template.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Todos los posts publicados, paginados
    Page<Post> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Posts de un autor concreto
    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    // Búsqueda por título (LIKE, case-insensitive)
    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) AND p.published = true")
    Page<Post> searchByTitle(@Param("query") String query, Pageable pageable);

    // Posts por tag
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName AND p.published = true")
    List<Post> findByTagName(@Param("tagName") String tagName);

    // Contador de posts publicados por autor
    long countByAuthorAndPublishedTrue(User author);
}