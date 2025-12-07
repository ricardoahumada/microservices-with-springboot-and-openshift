package com.mutualidad.afiliado.infrastructure.adapter.input.rest;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistrarAfiliadoRequest {
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private String direccion;
    private String codigoPostal;
    private String provincia;
    private String codigoEmpresa;
}
