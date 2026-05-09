package com.template.service;

import com.template.dto.RegisterRequest;
import com.template.entity.Role;
import com.template.entity.User;
import com.template.exception.BadRequestException;
import com.template.repository.RoleRepository;
import com.template.repository.UserRepository;
import com.template.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del UserServiceImpl con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("test");
        validRequest.setEmail("test@test.com");
        validRequest.setPassword("123456");
        validRequest.setFullName("Test User");
    }

    @Test
    void register_creaUsuario_cuandoTodoEsValido() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(roleRepository.findByName(Role.ERole.ROLE_USER))
                .thenReturn(Optional.of(new Role(Role.ERole.ROLE_USER)));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(validRequest);

        assertEquals("test", saved.getUsername());
        assertEquals("hashed", saved.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_lanzaExcepcion_cuandoUsernameExiste() {
        when(userRepository.existsByUsername("test")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.register(validRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_lanzaExcepcion_cuandoEmailExiste() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.register(validRequest));
    }
}
