package com.template.security;

import com.template.security.JwtUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils")
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    private static final String SECRET =
            "TestSecretKey12345TestSecretKey12345TestSecretKey12";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hora

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", EXPIRATION_MS);
    }

    private UserDetails user(String username) {
        return new User(username, "pass", Collections.emptyList());
    }

    @Test
    @DisplayName("generateToken() devuelve un token no nulo ni vacio")
    void generateToken_noEsNulo() {
        String token = jwtUtils.generateToken(user("jon"));
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("getUsernameFromToken() extrae el username correcto")
    void getUsernameFromToken_extraeUsername() {
        String token = jwtUtils.generateToken(user("jon"));
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("jon");
    }

    @Test
    @DisplayName("isTokenValid() devuelve true para token recien generado")
    void isTokenValid_trueParaTokenFresco() {
        String token = jwtUtils.generateToken(user("jon"));
        assertThat(jwtUtils.isTokenValid(token, user("jon"))).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() devuelve false con usuario distinto")
    void isTokenValid_falseParaUsuarioDistinto() {
        String token = jwtUtils.generateToken(user("jon"));
        assertThat(jwtUtils.isTokenValid(token, user("otro"))).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() devuelve false para token expirado")
    void isTokenValid_falseParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1L);
        String token = jwtUtils.generateToken(user("jon"));
        assertThat(jwtUtils.isTokenValid(token, user("jon"))).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() devuelve false para token manipulado")
    void isTokenValid_falseParaTokenManipulado() {
        String token = jwtUtils.generateToken(user("jon"));
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtUtils.isTokenValid(tampered, user("jon"))).isFalse();
    }

    @Test
    @DisplayName("Dos tokens generados en el mismo segundo son iguales (mismo iat)")
    void generarDosTokens_mismosegundo_sonIguales() {
        UserDetails u = user("jon");
        String t1 = jwtUtils.generateToken(u);
        String t2 = jwtUtils.generateToken(u);
        // JWT iat es precision de segundos, misma firma => mismo token
        assertThat(t1).isEqualTo(t2);
    }

    @Test
    @DisplayName("Dos tokens generados en segundos distintos son distintos")
    void generarDosTokens_sonDistintos() throws InterruptedException {
        UserDetails u = user("jon");
        String t1 = jwtUtils.generateToken(u);
        Thread.sleep(1100);   // esperar al siguiente segundo
        String t2 = jwtUtils.generateToken(u);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("Token con firma diferente es invalido")
    void tokenConFirmaDistinta_esInvalido() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "OtroSecretoCompletamenteDiferenteXXXXXXXXXXXXXXX12");
        String tokenOtroSecreto = jwtUtils.generateToken(user("jon"));

        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        assertThat(jwtUtils.isTokenValid(tokenOtroSecreto, user("jon"))).isFalse();
    }

    @Test
    @DisplayName("getUsernameFromToken() lanza excepcion con token invalido")
    void getUsernameFromToken_lanzaExcepcion_tokenInvalido() {
        assertThatThrownBy(() -> jwtUtils.extractUsername("token.invalido.aqui"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("isTokenValid() devuelve false para cadena vacia")
    void isTokenValid_falseParaCadenaVacia() {
        assertThat(jwtUtils.isTokenValid("", user("jon"))).isFalse();
    }
}
