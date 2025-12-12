package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.AsignarBeneficioCommand;
import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.BeneficioAggregate;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioAsignadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsignarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CommandResult handle(@Valid AsignarBeneficioCommand cmd) {
        log.info("Procesando asignacion de beneficio: tipo={}, afiliado={}",
            cmd.getTipoBeneficio(), cmd.getAfiliadoId());

        // Validar que no exista beneficio activo del mismo tipo
        List<Beneficio> beneficiosActivos = repository.findActivosByAfiliadoAndTipo(
            cmd.getAfiliadoId(), cmd.getTipoBeneficio()
        );

        if (!beneficiosActivos.isEmpty()) {
            log.warn("Ya existe beneficio activo del mismo tipo para afiliado: {}", cmd.getAfiliadoId());
            return CommandResult.failure("Ya existe un beneficio activo de tipo " + cmd.getTipoBeneficio());
        }

        // Validar monto si el tipo lo requiere
        if (cmd.getTipoBeneficio().requiereMonto() && cmd.getMonto() == null) {
            return CommandResult.failure("Este tipo de beneficio requiere especificar monto");
        }

        // Crear aggregate
        BeneficioAggregate aggregate = BeneficioAggregate.crear(
            cmd.getAfiliadoId(),
            cmd.getTipoBeneficio(),
            cmd.getFechaInicio(),
            cmd.getFechaFin(),
            cmd.getMonto(),
            cmd.getDescripcion(),
            cmd.getSolicitadoPor()
        );

        Beneficio beneficio = aggregate.getBeneficio();
        repository.save(beneficio);

        eventPublisher.publish(new BeneficioAsignadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            beneficio.getTipoBeneficio().name(),
            beneficio.getFechaInicio(),
            beneficio.getMonto()
        ));

        log.info("Beneficio asignado exitosamente: id={}", beneficio.getId());
        return CommandResult.success(beneficio.getId());
    }
}
