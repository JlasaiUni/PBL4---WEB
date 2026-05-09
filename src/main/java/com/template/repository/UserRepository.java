package com.template.repository;

import com.template.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /** Para el dashboard: carga usuario + posts + tags de posts en un solo JOIN. */
    @EntityGraph(attributePaths = {"posts", "posts.tags", "roles"})
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameWithPosts(@Param("username") String username);

    // Busca por username O email (para login con cualquiera de los dos)
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username,
                                         @Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
