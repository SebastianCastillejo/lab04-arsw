package co.edu.eci.blueprints.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "Respuesta uniforme de la API")
public record ApiResponse<T>(
        @Schema(description = "Codigo HTTP de la respuesta", example = "200") int code,
        @Schema(description = "Mensaje descriptivo", example = "execute ok") String message,
        @Schema(description = "Carga util; null cuando hubo error") T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "execute ok", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), "blueprint created", data);
    }

    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(HttpStatus.ACCEPTED.value(), "blueprint updated", data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message, null);
    }
}
