package co.edu.eci.blueprints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la API sobre el perfil "default" (persistencia en memoria),
 * de modo que no requieren PostgreSQL levantado.
 *
 * Verifican las buenas practicas del punto 3: versionamiento del path,
 * codigos HTTP correctos y formato uniforme ApiResponse.
 */
@SpringBootTest
@ActiveProfiles("default")
class BlueprintsAPIControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    /** Todas las peticiones llevan un JWT de prueba con ambos scopes. */
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(j -> j.claim("scope", "blueprints.read blueprints.write"))))
                .build();
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                .perform(get("/api/v1/blueprints"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloLectura_puedeConsultarPeroNoCrear() throws Exception {
        MockMvc readOnly = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(j -> j.claim("scope", "blueprints.read"))))
                .build();

        readOnly.perform(get("/api/v1/blueprints/john/house"))
                .andExpect(status().isOk());

        readOnly.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"tester","name":"bp403","points":[]}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        readOnly.perform(put("/api/v1/blueprints/john/house/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x":1,"y":1}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_devuelve200YFormatoApiResponse() throws Exception {
        mvc.perform(get("/api/v1/blueprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("execute ok"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getBlueprint_existente_devuelve200ConSusPuntos() throws Exception {
        mvc.perform(get("/api/v1/blueprints/john/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author").value("john"))
                .andExpect(jsonPath("$.data.name").value("house"))
                .andExpect(jsonPath("$.data.points.length()").value(4));
    }

    @Test
    void getBlueprint_inexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/v1/blueprints/nadie/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getByAuthor_sinBlueprints_devuelve404() throws Exception {
        mvc.perform(get("/api/v1/blueprints/nadie"))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_creaBlueprint_devuelve201ConLocation() throws Exception {
        mvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"tester","name":"bp201","points":[{"x":1,"y":1}]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/blueprints/tester/bp201"))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void post_duplicado_devuelve409() throws Exception {
        String body = """
                {"author":"tester","name":"bp409","points":[]}""";

        mvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void post_camposVacios_devuelve400ConDetalleDeCampos() throws Exception {
        mvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"","name":"","points":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation failed"))
                .andExpect(jsonPath("$.data.author").exists())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    void post_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/v1/blueprints")
                        .contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putPoint_devuelve202YElPuntoQuedaAlFinal() throws Exception {
        mvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"author":"tester","name":"bp202","points":[{"x":1,"y":1}]}"""))
                .andExpect(status().isCreated());

        mvc.perform(put("/api/v1/blueprints/tester/bp202/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x":9,"y":9}"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[1].x").value(9));
    }

    @Test
    void putPoint_blueprintInexistente_devuelve404() throws Exception {
        mvc.perform(put("/api/v1/blueprints/nadie/nada/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x":1,"y":1}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void rutaInexistente_devuelve404ConFormatoApiResponse() throws Exception {
        mvc.perform(get("/api/v1/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void verboNoSoportado_devuelve405() throws Exception {
        mvc.perform(delete("/api/v1/blueprints/john/house"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }
}
