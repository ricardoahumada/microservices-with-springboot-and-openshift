package com.mutualidad.afiliado.infrastructure.persistence;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AfiliadoJpaRepository extends JpaRepository<Afiliado, Long> {

    @Query("SELECT a FROM Afiliado a WHERE a.dni.valor = :dni")
    Optional<Afiliado> findByDni(@Param("dni") String dni);

    List<Afiliado> findByEstado(EstadoAfiliado estado);

    @Query("SELECT a FROM Afiliado a WHERE LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :apellidos, '%'))")
    List<Afiliado> findByApellidosContaining(@Param("apellidos") String apellidos);

    boolean existsByEmail(String email);
}
