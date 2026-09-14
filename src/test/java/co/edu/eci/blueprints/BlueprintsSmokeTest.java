package co.edu.eci.blueprints;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Perfil "default": las pruebas corren contra InMemoryBlueprintPersistence,
 * de modo que no requieren una base de datos levantada.
 */
@SpringBootTest
@ActiveProfiles("default")
class BlueprintsSmokeTest {
    @Test void contextLoads() {}
}
