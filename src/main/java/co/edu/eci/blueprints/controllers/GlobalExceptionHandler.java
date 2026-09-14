package co.edu.eci.blueprints.controllers;

import co.edu.eci.blueprints.dto.ApiResponse;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 el blueprint o el autor solicitado no existe */
    @ExceptionHandler(BlueprintNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(BlueprintNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /**
     * 409 el recurso ya existe
     */
    @ExceptionHandler(BlueprintPersistenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(BlueprintPersistenceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
    }

    /** 400: fallo la validacion de @Valid; se devuelve campo -> motivo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity.badRequest().body(
                new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "validation failed", errors));
    }

    /** 400 el cuerpo no es JSON valido o no se puede convertir al tipo esperado */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "malformed request body"));
    }

    /**
     * 404: la ruta no corresponde a ningun endpoint
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, "no endpoint for " + ex.getResourcePath()));
    }

    /** 405 la ruta existe pero no acepta ese verbo HTTP */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED,
                        "method " + ex.getMethod() + " not supported for this endpoint"));
    }

    /** 400 un parametro no se pudo convertir al tipo esperado */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                "invalid value for parameter '" + ex.getName() + "'"));
    }

    /** 403 el token es valido pero no tiene el scope que exige @PreAuthorize */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, "insufficient scope"));
    }

    /** 500 red de seguridad. Se registra el detalle y no se filtra al cliente */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error"));
    }
}
