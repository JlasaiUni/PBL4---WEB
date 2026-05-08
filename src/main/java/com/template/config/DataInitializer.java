package com.template.config;

import com.template.entity.Role;
import com.template.entity.User;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Crea el usuario admin por defecto la primera vez que arranca la app.
 * Credenciales:  admin / admin123
 *
 * Idempotente: si el admin ya existe, no hace nada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("Usuario 'admin' ya existe — saltando inicialización.");
            return;
        }

        Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_ADMIN no encontrado. ¿Se ejecutaron las migraciones Flyway?"));

        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_USER no encontrado. ¿Se ejecutaron las migraciones Flyway?"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);

        User admin = User.builder()
                .username("admin")
                .email("admin@template.local")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Administrador")
                .enabled(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();

        userRepository.save(admin);
        log.info("==========================================================");
        log.info("  Usuario admin creado:  username=admin   password=admin123");
        log.info("==========================================================");
    }
}
