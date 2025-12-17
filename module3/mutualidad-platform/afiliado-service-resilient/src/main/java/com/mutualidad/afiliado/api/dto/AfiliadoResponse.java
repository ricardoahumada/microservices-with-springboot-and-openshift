package com.mutualidad.afiliado.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfiliadoResponse {
    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String estado;
    private String empresaId;
    private LocalDateTime fechaCreacion;
    private List<BeneficioDto> beneficios;
    private String mensaje;
}
