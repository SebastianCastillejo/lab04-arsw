package co.edu.eci.blueprints.controllers;

import co.edu.eci.blueprints.dto.ApiResponse;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import co.edu.eci.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Consulta y gestion de planos y sus puntos")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) {
        this.services = services;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @Operation(summary = "Lista todos los blueprints",
            description = "Devuelve el conjunto completo de planos registrados. Si no hay ninguno, devuelve una lista vacia.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Consulta exitosa",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(services.getAllBlueprints()));
    }

    @GetMapping("/{author}")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @Operation(summary = "Lista los blueprints de un autor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "El autor no tiene blueprints registrados",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(
            @Parameter(description = "Autor del plano", example = "john")
            @PathVariable String author) throws BlueprintNotFoundException {

        return ResponseEntity.ok(ApiResponse.ok(services.getBlueprintsByAuthor(author)));
    }

    @GetMapping("/{author}/{bpname}")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.read')")
    @Operation(summary = "Obtiene un blueprint concreto",
            description = "Los puntos se devuelven procesados por el filtro activo (identity, redundancy o undersampling).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "El blueprint no existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @Parameter(description = "Autor del plano", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del plano", example = "house") @PathVariable String bpname)
            throws BlueprintNotFoundException {

        return ResponseEntity.ok(ApiResponse.ok(services.getBlueprint(author, bpname)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    @Operation(summary = "Registra un nuevo blueprint",
            description = "Devuelve 201 con la cabecera Location apuntando al recurso creado.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Blueprint creado")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Datos invalidos (autor o nombre vacios, cuerpo mal formado)",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Ya existe un blueprint con ese autor y nombre",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req)
            throws BlueprintPersistenceException {

        Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
        services.addNewBlueprint(bp);

        URI location = URI.create("/api/v1/blueprints/%s/%s".formatted(req.author(), req.name()));
        return ResponseEntity.created(location).body(ApiResponse.created(bp));
    }

    @PutMapping("/{author}/{bpname}/points")
    @PreAuthorize("hasAuthority('SCOPE_blueprints.write')")
    @Operation(summary = "Agrega un punto al final de un blueprint")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Punto agregado")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Cuerpo mal formado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "El blueprint no existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Blueprint>> addPoint(
            @Parameter(description = "Autor del plano", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del plano", example = "house") @PathVariable String bpname,
            @Valid @RequestBody Point p) throws BlueprintNotFoundException {

        services.addPoint(author, bpname, p.x(), p.y());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(services.getBlueprint(author, bpname)));
    }

    @Schema(description = "Peticion de creacion de un blueprint")
    public record NewBlueprintRequest(
            @Schema(description = "Autor del plano", example = "john")
            @NotBlank(message = "author must not be blank") String author,

            @Schema(description = "Nombre del plano", example = "kitchen")
            @NotBlank(message = "name must not be blank") String name,

            @Schema(description = "Secuencia ordenada de puntos")
            @NotNull(message = "points must not be null") @Valid List<Point> points) { }
}
