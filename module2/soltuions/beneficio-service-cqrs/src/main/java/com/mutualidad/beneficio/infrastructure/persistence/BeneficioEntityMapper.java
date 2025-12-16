package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.EstadoBeneficio;
import org.springframework.stereotype.Component;

@Component
public class BeneficioEntityMapper {

    public BeneficioEntity toEntity(Beneficio beneficio) {
        return BeneficioEntity.builder()
            .id(beneficio.getId())
            .afiliadoId(beneficio.getAfiliadoId())
            .tipoBeneficio(beneficio.getTipoBeneficio().name())
            .estado(beneficio.getEstado().name())
            .fechaInicio(beneficio.getFechaInicio())
            .fechaFin(beneficio.getFechaFin())
            .monto(beneficio.getMonto())
            .descripcion(beneficio.getDescripcion())
            .solicitadoPor(beneficio.getSolicitadoPor())
            .fechaCreacion(beneficio.getFechaCreacion())
            .fechaRevocacion(beneficio.getFechaRevocacion())
            .motivoRevocacion(beneficio.getMotivoRevocacion())
            .revocadoPor(beneficio.getRevocadoPor())
            .fechaSuspension(beneficio.getFechaSuspension())
            .motivoSuspension(beneficio.getMotivoSuspension())
            .suspendidoPor(beneficio.getSuspendidoPor())
            .build();
    }

    public Beneficio toDomain(BeneficioEntity entity) {
        return Beneficio.builder()
            .id(entity.getId())
            .afiliadoId(entity.getAfiliadoId())
            .tipoBeneficio(TipoBeneficio.valueOf(entity.getTipoBeneficio()))
            .estado(EstadoBeneficio.valueOf(entity.getEstado()))
            .fechaInicio(entity.getFechaInicio())
            .fechaFin(entity.getFechaFin())
            .monto(entity.getMonto())
            .descripcion(entity.getDescripcion())
            .solicitadoPor(entity.getSolicitadoPor())
            .fechaCreacion(entity.getFechaCreacion())
            .fechaRevocacion(entity.getFechaRevocacion())
            .motivoRevocacion(entity.getMotivoRevocacion())
            .revocadoPor(entity.getRevocadoPor())
            .fechaSuspension(entity.getFechaSuspension())
            .motivoSuspension(entity.getMotivoSuspension())
            .suspendidoPor(entity.getSuspendidoPor())
            .build();
    }
}
