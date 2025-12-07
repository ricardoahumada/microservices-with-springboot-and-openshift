package com.mutualidad.beneficio.infrastructure.event;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioAsignadoEvent;
import com.mutualidad.beneficio.event.BeneficioModificadoEvent;
import com.mutualidad.beneficio.event.BeneficioRevocadoEvent;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeneficioProjection {

    private final BeneficioReadRepository readRepository;
    private final BeneficioWriteRepository writeRepository;

    @EventListener
    @Transactional
    public void on(BeneficioAsignadoEvent event) {
        log.info("Proyectando BENEFICIO_ASIGNADO: {}", event.getBeneficioId());

        Beneficio beneficio = writeRepository.findById(event.getBeneficioId())
            .orElseThrow(() -> new IllegalStateException("Beneficio no encontrado"));

        TipoBeneficio tipo = beneficio.getTipoBeneficio();

        BeneficioReadModel readModel = BeneficioReadModel.builder()
            .id(beneficio.getId())
            .afiliadoId(beneficio.getAfiliadoId())
            .tipoBeneficio(tipo.name())
            .tipoBeneficioDescripcion(tipo.getDescripcion())
            .estado(beneficio.getEstado().name())
            .fechaInicio(beneficio.getFechaInicio())
            .fechaFin(beneficio.getFechaFin())
            .monto(beneficio.getMonto())
            .montoFormateado(formatearMonto(beneficio.getMonto()))
            .descripcion(beneficio.getDescripcion())
            .estaVigente(beneficio.estaVigente(LocalDate.now()))
            .diasRestantes(calcularDiasRestantes(beneficio.getFechaFin()))
            .fechaCreacion(beneficio.getFechaCreacion())
            .ultimaActualizacion(LocalDateTime.now())
            .build();

        readRepository.save(readModel);
        log.info("Read model creado para beneficio: {}", beneficio.getId());
    }

    @EventListener
    @Transactional
    public void on(BeneficioModificadoEvent event) {
        log.info("Proyectando BENEFICIO_MODIFICADO: {}", event.getBeneficioId());

        Beneficio beneficio = writeRepository.findById(event.getBeneficioId())
            .orElseThrow(() -> new IllegalStateException("Beneficio no encontrado"));

        readRepository.findById(event.getBeneficioId()).ifPresent(readModel -> {
            readModel.setMonto(beneficio.getMonto());
            readModel.setMontoFormateado(formatearMonto(beneficio.getMonto()));
            readModel.setFechaFin(beneficio.getFechaFin());
            readModel.setDescripcion(beneficio.getDescripcion());
            readModel.setEstaVigente(beneficio.estaVigente(LocalDate.now()));
            readModel.setDiasRestantes(calcularDiasRestantes(beneficio.getFechaFin()));
            readModel.setUltimaActualizacion(LocalDateTime.now());
            readRepository.save(readModel);
        });
    }

    @EventListener
    @Transactional
    public void on(BeneficioRevocadoEvent event) {
        log.info("Proyectando BENEFICIO_REVOCADO: {}", event.getBeneficioId());

        readRepository.findById(event.getBeneficioId()).ifPresent(readModel -> {
            readModel.setEstado("REVOCADO");
            readModel.setEstaVigente(false);
            readModel.setDiasRestantes(null);
            readModel.setUltimaActualizacion(LocalDateTime.now());
            readRepository.save(readModel);
        });
    }

    private String formatearMonto(java.math.BigDecimal monto) {
        if (monto == null) return null;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return formatter.format(monto);
    }

    private Integer calcularDiasRestantes(LocalDate fechaFin) {
        if (fechaFin == null) return null;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
        return dias >= 0 ? (int) dias : null;
    }
}
