package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "afiliados", indexes = {
    @Index(name = "idx_documento", columnList = "tipo_documento, numero_documento", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfiliadoEntity {

    @Id
    private String id;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false)
    private String numeroDocumento;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "primer_apellido", nullable = false)
    private String primerApellido;

    @Column(name = "segundo_apellido")
    private String segundoApellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    private String email;
    private String telefono;
    private String direccion;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private String provincia;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "motivo_baja")
    private String motivoBaja;

    @Column(name = "codigo_empresa", nullable = false)
    private String codigoEmpresa;
}
