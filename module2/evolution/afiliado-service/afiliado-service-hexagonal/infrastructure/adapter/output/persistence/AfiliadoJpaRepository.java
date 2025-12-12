package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AfiliadoJpaRepository extends JpaRepository<AfiliadoEntity, String> {

    Optional<AfiliadoEntity> findByTipoDocumentoAndNumeroDocumento(
        String tipoDocumento, 
        String numeroDocumento
    );

    boolean existsByTipoDocumentoAndNumeroDocumento(
        String tipoDocumento, 
        String numeroDocumento
    );

    List<AfiliadoEntity> findByEstado(String estado);

    List<AfiliadoEntity> findByCodigoEmpresa(String codigoEmpresa);
}
