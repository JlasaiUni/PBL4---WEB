package com.template.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // habilita @PreAuthorize / @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // ── Recursos públicos ────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/",
            "/auth/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/webjars/**",
            "/actuator/health",
            "/ws/**"              // WebSocket endpoint
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: activo para vistas MVC; desactívalo sólo en APIs REST puras
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/api/**", "/ws/**"))

            // Autorización de rutas
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated())

            // Formulario de login
            .formLogin(form -> form
                    .loginPage("/auth/login")
                    .loginProcessingUrl("/auth/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/auth/login?error=true")
                    .permitAll())

            // Logout
            .logout(logout -> logout
                    .logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/auth/login?logout=true")
                    .deleteCookies("JSESSIONID", "jwtToken")
                    .invalidateHttpSession(true)
                    .permitAll())

            // Recuerda la sesión (remember-me)
            .rememberMe(rm -> rm
                    .key("uniqueAndSecretKey")
                    .tokenValiditySeconds(86400))

            // Para endpoints REST: stateless + JWT
            .sessionManagement(sm -> sm
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // Añadir el filtro JWT antes del filtro de usuario/contraseña
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Página de acceso denegado
            .exceptionHandling(ex -> ex
                    .accessDeniedPage("/error/403"));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}