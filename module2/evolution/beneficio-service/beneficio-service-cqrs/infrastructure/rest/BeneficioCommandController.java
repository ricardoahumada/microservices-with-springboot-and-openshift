package com.mutualidad.beneficio.infrastructure.rest;

import com.mutualidad.beneficio.command.api.*;
import com.mutualidad.beneficio.command.handler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/beneficios/commands")
@RequiredArgsConstructor
public class BeneficioCommandController {

    private final AsignarBeneficioHandler asignarHandler;
    private final RevocarBeneficioHandler revocarHandler;
    private final ModificarBeneficioHandler modificarHandler;

    @PostMapping("/asignar")
    public ResponseEntity<CommandResult> asignar(@Valid @RequestBody AsignarBeneficioCommand command) {
        CommandResult result = asignarHandler.handle(command);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/revocar")
    public ResponseEntity<CommandResult> revocar(@Valid @RequestBody RevocarBeneficioCommand command) {
        CommandResult result = revocarHandler.handle(command);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/modificar")
    public ResponseEntity<CommandResult> modificar(@Valid @RequestBody ModificarBeneficioCommand command) {
        CommandResult result = modificarHandler.handle(command);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}
