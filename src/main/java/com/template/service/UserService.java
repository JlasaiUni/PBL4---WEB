package com.template.service;

import com.template.dto.RegisterRequest;
import com.template.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(RegisterRequest request);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameWithPosts(String username);
    Optional<User> findById(Long id);
    List<User> findAll();
    void deleteById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
