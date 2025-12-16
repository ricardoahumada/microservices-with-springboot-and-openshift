package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.model.ResumenBeneficiosReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResumenBeneficiosHandler {

    private final BeneficioReadRepository repository;

    public ResumenBeneficiosReadModel handle(String afiliadoId) {
        log.debug("Generando resumen de beneficios para: {}", afiliadoId);

        long activos = repository.countByAfiliadoIdAndEstado(afiliadoId, "ACTIVO");
        long suspendidos = repository.countByAfiliadoIdAndEstado(afiliadoId, "SUSPENDIDO");
        long revocados = repository.countByAfiliadoIdAndEstado(afiliadoId, "REVOCADO");

        BigDecimal montoTotal = repository.sumMontoActivoByAfiliado(afiliadoId);
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

        return ResumenBeneficiosReadModel.builder()
            .afiliadoId(afiliadoId)
            .totalBeneficios((int)(activos + suspendidos + revocados))
            .beneficiosActivos((int) activos)
            .beneficiosSuspendidos((int) suspendidos)
            .beneficiosRevocados((int) revocados)
            .montoTotalActivo(montoTotal)
            .montoTotalFormateado(formatter.format(montoTotal))
            .build();
    }
}
