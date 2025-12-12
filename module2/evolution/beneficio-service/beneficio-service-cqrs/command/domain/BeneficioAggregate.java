package com.mutualidad.beneficio.command.domain;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class BeneficioAggregate {

    private final Beneficio beneficio;

    private BeneficioAggregate(Beneficio beneficio) {
        this.beneficio = beneficio;
    }

    public static BeneficioAggregate crear(
            String afiliadoId,
            TipoBeneficio tipoBeneficio,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal monto,
            String descripcion,
            String solicitadoPor) {

        validarFechas(fechaInicio, fechaFin);
        validarMonto(tipoBeneficio, monto);

        Beneficio beneficio = Beneficio.builder()
            .id(UUID.randomUUID().toString())
            .afiliadoId(afiliadoId)
            .tipoBeneficio(tipoBeneficio)
            .estado(EstadoBeneficio.ACTIVO)
            .fechaInicio(fechaInicio)
            .fechaFin(fechaFin)
            .monto(monto)
            .descripcion(descripcion)
            .solicitadoPor(solicitadoPor)
            .fechaCreacion(LocalDate.now())
            .build();

        return new BeneficioAggregate(beneficio);
    }

    public static BeneficioAggregate fromExisting(Beneficio beneficio) {
        return new BeneficioAggregate(beneficio);
    }

    private static void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private static void validarMonto(TipoBeneficio tipo, BigDecimal monto) {
        if (tipo.requiereMonto() && (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("El tipo de beneficio " + tipo + " requiere un monto positivo");
        }
    }
}
