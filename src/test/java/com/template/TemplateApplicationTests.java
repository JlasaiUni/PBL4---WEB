package com.template;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: comprueba que el contexto de Spring arranca sin errores.
 */
@SpringBootTest
@ActiveProfiles("dev")
class TemplateApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto no carga, este test falla.
    }
}
