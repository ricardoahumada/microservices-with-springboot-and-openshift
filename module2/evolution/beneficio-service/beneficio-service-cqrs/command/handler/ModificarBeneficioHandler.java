package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.api.ModificarBeneficioCommand;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioModificadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModificarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CommandResult handle(@Valid ModificarBeneficioCommand cmd) {
        log.info("Procesando modificacion de beneficio: id={}", cmd.getBeneficioId());

        Beneficio beneficio = repository.findById(cmd.getBeneficioId()).orElse(null);

        if (beneficio == null) {
            return CommandResult.failure("Beneficio no encontrado");
        }

        if (!beneficio.esModificable()) {
            return CommandResult.failure("El beneficio en estado " + beneficio.getEstado() + " no es modificable");
        }

        if (cmd.getNuevoMonto() != null) {
            beneficio.actualizarMonto(cmd.getNuevoMonto());
        }
        if (cmd.getNuevaFechaFin() != null) {
            beneficio.actualizarFechaFin(cmd.getNuevaFechaFin());
        }
        if (cmd.getNuevaDescripcion() != null) {
            beneficio.actualizarDescripcion(cmd.getNuevaDescripcion());
        }

        repository.save(beneficio);

        eventPublisher.publish(new BeneficioModificadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            cmd.getMotivo(),
            cmd.getModificadoPor()
        ));

        log.info("Beneficio modificado exitosamente: id={}", cmd.getBeneficioId());
        return CommandResult.success(cmd.getBeneficioId());
    }
}
