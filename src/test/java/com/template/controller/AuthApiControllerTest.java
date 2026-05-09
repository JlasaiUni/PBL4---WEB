package com.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.dto.LoginRequest;
import com.template.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración con MockMvc.
 *
 * Levanta el contexto completo con H2 (perfil "dev").
 * Cubre:
 *   - POST /api/auth/register  (éxito, usuario duplicado, datos inválidos)
 *   - POST /api/auth/login     (éxito, credenciales incorrectas)
 *   - Acceso a /admin/**       (sin auth, ROLE_USER, ROLE_ADMIN)
 *   - Acceso a /api/v1/posts   (autenticado vs anónimo)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("AuthApiController — integración MockMvc")
class AuthApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ══════════════════════════════════════════════════════════
    //  POST /api/auth/register
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("register() devuelve 200 con datos válidos y usuario nuevo")
    void register_devuelve200_cuandoDatosValidos() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("nuevouser");
        req.setEmail("nuevo@test.com");
        req.setPassword("password123");
        req.setFullName("Nuevo User");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("register() devuelve 400 cuando el username ya existe")
    void register_devuelve400_cuandoUsernameYaExiste() throws Exception {
        // Primero registramos el usuario
        RegisterRequest first = new RegisterRequest();
        first.setUsername("duplicado");
        first.setEmail("duplicado@test.com");
        first.setPassword("password123");
        first.setFullName("Duplicado");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // Segundo registro con el mismo username
        RegisterRequest second = new RegisterRequest();
        second.setUsername("duplicado");
        second.setEmail("otro@test.com");
        second.setPassword("password123");
        second.setFullName("Otro");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register() devuelve 400 cuando los datos no pasan validación")
    void register_devuelve400_cuandoDatosInvalidos() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("x");           // demasiado corto
        req.setEmail("no-es-un-email");
        req.setPassword("123");         // demasiado corta
        req.setFullName("");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════
    //  POST /api/auth/login
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("login() devuelve JWT válido con credenciales del admin por defecto")
    void login_devuelveJwt_cuandoCredencialesCorrectas() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("admin");
        req.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("login() devuelve 401 con credenciales incorrectas")
    void login_devuelve401_cuandoPasswordIncorrecta() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("admin");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login() devuelve 400 con body vacío")
    void login_devuelve400_cuandoBodyVacio() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════
    //  Acceso por rol — GET /admin
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /admin sin autenticación redirige a login")
    void adminPanel_sinAuth_redirigueALogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("GET /admin con ROLE_USER devuelve 403")
    void adminPanel_conRoleUser_devuelve403() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    @DisplayName("GET /admin con ROLE_ADMIN devuelve 200")
    void adminPanel_conRoleAdmin_devuelve200() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════
    //  Acceso por rol — POST /api/v1/posts
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/v1/posts sin autenticación devuelve 401 o 403")
    void createPost_sinAuth_devuelveNoAutorizado() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Contenido\",\"published\":false}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    @DisplayName("GET /api/v1/posts con usuario autenticado devuelve 200")
    void getPosts_conAuth_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/posts sin autenticación devuelve 200 (endpoint público)")
    void getPosts_sinAuth_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk());
    }
}
