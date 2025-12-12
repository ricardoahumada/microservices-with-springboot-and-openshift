package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.api.RevocarBeneficioCommand;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.EstadoBeneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioRevocadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevocarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CommandResult handle(@Valid RevocarBeneficioCommand cmd) {
        log.info("Procesando revocacion de beneficio: id={}", cmd.getBeneficioId());

        Beneficio beneficio = repository.findById(cmd.getBeneficioId()).orElse(null);

        if (beneficio == null) {
            return CommandResult.failure("Beneficio no encontrado");
        }

        if (beneficio.getEstado() == EstadoBeneficio.REVOCADO) {
            return CommandResult.failure("El beneficio ya esta revocado");
        }

        if (beneficio.getEstado() == EstadoBeneficio.EXPIRADO) {
            return CommandResult.failure("No se puede revocar un beneficio expirado");
        }

        LocalDate fechaEfectiva = cmd.getFechaEfectiva() != null ? cmd.getFechaEfectiva() : LocalDate.now();
        beneficio.revocar(cmd.getMotivo(), fechaEfectiva, cmd.getRevocadoPor());
        repository.save(beneficio);

        eventPublisher.publish(new BeneficioRevocadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            cmd.getMotivo(),
            fechaEfectiva
        ));

        log.info("Beneficio revocado exitosamente: id={}", cmd.getBeneficioId());
        return CommandResult.success(cmd.getBeneficioId());
    }
}
