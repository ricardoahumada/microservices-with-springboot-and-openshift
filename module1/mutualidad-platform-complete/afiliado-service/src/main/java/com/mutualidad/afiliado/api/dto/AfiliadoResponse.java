package com.mutualidad.afiliado.api.dto;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AfiliadoResponse {

    private Long id;
    private String dni;
    private String nombre;
    private String apellidos;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private EstadoAfiliado estado;
    private LocalDate fechaAlta;
    private LocalDate fechaBaja;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AfiliadoResponse fromEntity(Afiliado afiliado) {
        return AfiliadoResponse.builder()
                .id(afiliado.getId())
                .dni(afiliado.getDni().getValor())
                .nombre(afiliado.getNombre())
                .apellidos(afiliado.getApellidos())
                .nombreCompleto(afiliado.getNombreCompleto())
                .fechaNacimiento(afiliado.getFechaNacimiento())
                .email(afiliado.getEmail())
                .telefono(afiliado.getTelefono())
                .estado(afiliado.getEstado())
                .fechaAlta(afiliado.getFechaAlta())
                .fechaBaja(afiliado.getFechaBaja())
                .createdAt(afiliado.getCreatedAt())
                .updatedAt(afiliado.getUpdatedAt())
                .build();
    }
}
