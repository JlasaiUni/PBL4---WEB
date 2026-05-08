package com.template.service.impl;

import com.template.dto.AuthDTOs;
import com.template.entity.Role;
import com.template.entity.User;
import com.template.exception.BadRequestException;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import com.template.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(AuthDTOs.RegisterRequest request) {
        // Validaciones de negocio
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya existe: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado: " + request.getEmail());
        }

        // Rol por defecto: ROLE_USER
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado en BD. Ejecuta las migraciones Flyway."));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .enabled(true)
                .build();

        user.addRole(userRole);

        User saved = userRepository.save(user);
        log.info("Nuevo usuario registrado: {}", saved.getUsername());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("users")
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> {
                    // Forzar inicialización de colecciones lazy
                    // mientras la sesión Hibernate está abierta,
                    // para que la vista pueda recorrerlas sin LazyInitializationException.
                    u.getRoles().size();
                    u.getPosts().size();
                    return u;
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("users")
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("users")
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public void deleteById(Long id) {
        userRepository.deleteById(id);
        log.info("Usuario eliminado con id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}