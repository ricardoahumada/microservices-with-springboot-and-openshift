package com.mutualidad.afiliado.infrastructure.adapter.input.rest;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;
import com.mutualidad.afiliado.application.port.input.AfiliadoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoUseCase afiliadoUseCase;

    @PostMapping
    public ResponseEntity<AfiliadoDTO> registrar(
            @Valid @RequestBody RegistrarAfiliadoRequest request) {
        
        RegistrarAfiliadoCommand command = mapToCommand(request);
        AfiliadoDTO resultado = afiliadoUseCase.registrarAfiliado(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(resultado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AfiliadoDTO> consultarPorId(@PathVariable String id) {
        return afiliadoUseCase.consultarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/documento/{tipo}/{numero}")
    public ResponseEntity<AfiliadoDTO> consultarPorDocumento(
            @PathVariable String tipo,
            @PathVariable String numero) {
        return afiliadoUseCase.consultarPorDocumento(tipo, numero)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/baja")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable String id,
            @RequestBody DarDeBajaRequest request) {
        afiliadoUseCase.darDeBaja(id, request.getMotivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable String id) {
        afiliadoUseCase.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/contacto")
    public ResponseEntity<AfiliadoDTO> actualizarContacto(
            @PathVariable String id,
            @RequestBody ActualizarContactoRequest request) {
        AfiliadoDTO actualizado = afiliadoUseCase.actualizarContacto(
            id, request.getEmail(), request.getTelefono());
        return ResponseEntity.ok(actualizado);
    }

    private RegistrarAfiliadoCommand mapToCommand(RegistrarAfiliadoRequest request) {
        return RegistrarAfiliadoCommand.builder()
            .tipoDocumento(request.getTipoDocumento())
            .numeroDocumento(request.getNumeroDocumento())
            .nombre(request.getNombre())
            .primerApellido(request.getPrimerApellido())
            .segundoApellido(request.getSegundoApellido())
            .fechaNacimiento(request.getFechaNacimiento())
            .email(request.getEmail())
            .telefono(request.getTelefono())
            .direccion(request.getDireccion())
            .codigoPostal(request.getCodigoPostal())
            .provincia(request.getProvincia())
            .codigoEmpresa(request.getCodigoEmpresa())
            .build();
    }
}
