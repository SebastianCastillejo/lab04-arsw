package co.edu.eci.blueprints.auth;

import co.edu.eci.blueprints.security.InMemoryUserService;
import co.edu.eci.blueprints.security.RsaKeyProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Login didactico para emitir el JWT")
public class AuthController {

    private final JwtEncoder encoder;
    private final InMemoryUserService userService;
    private final RsaKeyProperties props;

    public AuthController(JwtEncoder encoder, InMemoryUserService userService, RsaKeyProperties props) {
        this.encoder = encoder;
        this.userService = userService;
        this.props = props;
    }

    @Schema(description = "Credenciales de login")
    public record LoginRequest(
            @Schema(description = "Usuario en memoria", example = "student") String username,
            @Schema(description = "Contrasena en texto plano", example = "student123") String password) {}

    @Schema(description = "Token emitido tras un login correcto")
    public record TokenResponse(
            @Schema(description = "JWT firmado con RS256") String access_token,
            @Schema(description = "Tipo de token", example = "Bearer") String token_type,
            @Schema(description = "Segundos hasta que vence", example = "30") long expires_in) {}

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Emite un JWT",
            description = "Endpoint publico. Valida usuario/contrasena en memoria y devuelve un Bearer firmado con RS256.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login correcto, se devuelve el token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Credenciales invalidas",
            content = @Content)
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (!userService.isValid(req.username(), req.password())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        Instant now = Instant.now();
        long ttl = props.tokenTtlSeconds() != null ? props.tokenTtlSeconds() : 3600;
        Instant exp = now.plusSeconds(ttl);

        String scope = userService.scopesOf(req.username());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(req.username())
                .claim("scope", scope)
                .build();

        JwsHeader jws = JwsHeader.with(() -> "RS256").build();
        String token = this.encoder.encode(JwtEncoderParameters.from(jws, claims)).getTokenValue();

        return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl));
    }
}
