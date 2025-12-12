package com.mutualidad.beneficio.command.repository;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import com.mutualidad.beneficio.command.domain.Beneficio;

import java.util.List;
import java.util.Optional;

public interface BeneficioWriteRepository {
    Beneficio save(Beneficio beneficio);
    Optional<Beneficio> findById(String id);
    List<Beneficio> findActivosByAfiliadoAndTipo(String afiliadoId, TipoBeneficio tipo);
    void deleteById(String id);
}
