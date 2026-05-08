package com.template.controller;

import com.template.dto.AuthDTOs;
import com.template.security.JwtUtils;
import com.template.security.UserDetailsServiceImpl;
import com.template.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API REST para autenticación con JWT.
 * Complementa el formulario MVC tradicional /auth/login.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.JwtResponse> login(@Valid @RequestBody AuthDTOs.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        AuthDTOs.JwtResponse body = AuthDTOs.JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .username(userDetails.getUsername())
                .roles(roles)
                .build();

        return ResponseEntity.ok(body);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDTOs.RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok().body("Usuario registrado correctamente");
    }
}
