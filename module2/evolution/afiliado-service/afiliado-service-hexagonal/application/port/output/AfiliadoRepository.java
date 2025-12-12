package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;

import java.util.Optional;

public interface AfiliadoRepository {

    Afiliado save(Afiliado afiliado);

    Optional<Afiliado> findById(String id);

    Optional<Afiliado> findByDocumento(Documento documento);

    boolean existsByDocumento(Documento documento);

    void deleteById(String id);
}
