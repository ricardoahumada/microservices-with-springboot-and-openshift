package com.mutualidad.afiliado.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AfiliadoDTO {
    private String id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private String direccion;
    private String estado;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private String motivoBaja;
    private String codigoEmpresa;
}
