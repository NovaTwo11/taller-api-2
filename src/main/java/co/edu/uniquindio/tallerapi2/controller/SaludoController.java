package co.edu.uniquindio.tallerapi2.controller;

import co.edu.uniquindio.tallerapi2.dto.ApiError;
import co.edu.uniquindio.tallerapi2.dto.SaludoRequest;
import co.edu.uniquindio.tallerapi2.dto.SaludoResponse;
import co.edu.uniquindio.tallerapi2.model.Saludo;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.SaludoRepository;
import co.edu.uniquindio.tallerapi2.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saludos")
@Tag(name = "Saludos", description = "Gestión de saludos enviados por los usuarios")
public class SaludoController {

    private final SaludoRepository saludoRepository;
    private final UsuarioRepository usuarioRepository;

    public SaludoController(SaludoRepository saludoRepository, UsuarioRepository usuarioRepository) {
        this.saludoRepository = saludoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Enviar un saludo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Saludo enviado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<?> enviarSaludo(
            @Valid @RequestBody SaludoRequest request,
            HttpServletRequest httpRequest) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(request.getUsuarioId());

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();
            Saludo saludo = new Saludo();
            saludo.setMensaje(request.getMensaje());
            saludo.setUsuario(usuario);
            saludo.setFecha(LocalDateTime.now());

            Saludo guardado = saludoRepository.save(saludo);

            SaludoResponse response = new SaludoResponse(
                    guardado.getId(),
                    guardado.getMensaje(),
                    guardado.getFecha(),
                    usuario.getNombre()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error al procesar saludo", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Listar todos los saludos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<SaludoResponse>> listarSaludos() {
        List<SaludoResponse> saludos = saludoRepository.findAll()
                .stream()
                .map(s -> new SaludoResponse(
                        s.getId(),
                        s.getMensaje(),
                        s.getFecha(),
                        s.getUsuario().getNombre()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(saludos);
    }

    @Operation(summary = "Obtener un saludo por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saludo encontrado"),
            @ApiResponse(responseCode = "404", description = "Saludo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerSaludo(
            @Parameter(description = "ID único del saludo", required = true)
            @PathVariable UUID id,
            HttpServletRequest request) {

        Optional<Saludo> saludoOpt = saludoRepository.findById(id);

        if (saludoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "Not Found", "Saludo no encontrado", request.getRequestURI()));
        }

        Saludo saludo = saludoOpt.get();
        return ResponseEntity.ok(new SaludoResponse(
                saludo.getId(), saludo.getMensaje(), saludo.getFecha(), saludo.getUsuario().getNombre()
        ));
    }

    @Operation(summary = "Eliminar saludo por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saludo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Saludo no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSaludo(
            @Parameter(description = "ID único del saludo", required = true)
            @PathVariable UUID id,
            HttpServletRequest request) {

        Optional<Saludo> saludoOpt = saludoRepository.findById(id);

        if (saludoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "Not Found", "Saludo no encontrado", request.getRequestURI()));
        }

        saludoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Saludo eliminado exitosamente"));
    }
}