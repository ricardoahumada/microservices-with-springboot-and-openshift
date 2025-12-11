package com.mutualidad.afiliado.repository;

import com.mutualidad.afiliado.entity.Afiliado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AfiliadoRepository extends JpaRepository<Afiliado, Long> {
    Optional<Afiliado> findByDni(String dni);
    boolean existsByDni(String dni);
}
