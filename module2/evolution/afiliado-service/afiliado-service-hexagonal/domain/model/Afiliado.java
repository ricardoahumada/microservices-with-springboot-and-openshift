package com.mutualidad.afiliado.domain.model;

import com.mutualidad.afiliado.domain.exception.EstadoInvalidoException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Afiliado {

    private final String id;
    private final Documento documento;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private String direccion;
    private String codigoPostal;
    private String provincia;
    private EstadoAfiliado estado;
    private final LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private String motivoBaja;
    private final String codigoEmpresa;

    public static Afiliado crear(
            Documento documento,
            String nombre,
            String primerApellido,
            String segundoApellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String direccion,
            String codigoPostal,
            String provincia,
            String codigoEmpresa) {

        validarDatosObligatorios(documento, nombre, primerApellido, fechaNacimiento, codigoEmpresa);
        validarEdadMinima(fechaNacimiento);

        return new Afiliado(
            UUID.randomUUID().toString(),
            documento,
            nombre,
            primerApellido,
            segundoApellido,
            fechaNacimiento,
            email,
            telefono,
            direccion,
            codigoPostal,
            provincia,
            EstadoAfiliado.PENDIENTE,
            LocalDateTime.now(),
            null,
            null,
            codigoEmpresa
        );
    }

    public static Afiliado reconstitute(
            String id,
            Documento documento,
            String nombre,
            String primerApellido,
            String segundoApellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String direccion,
            String codigoPostal,
            String provincia,
            EstadoAfiliado estado,
            LocalDateTime fechaAlta,
            LocalDateTime fechaBaja,
            String motivoBaja,
            String codigoEmpresa) {

        return new Afiliado(
            id, documento, nombre, primerApellido, segundoApellido,
            fechaNacimiento, email, telefono, direccion, codigoPostal,
            provincia, estado, fechaAlta, fechaBaja, motivoBaja, codigoEmpresa
        );
    }

    public void activar() {
        if (this.estado != EstadoAfiliado.PENDIENTE) {
            throw new EstadoInvalidoException(
                "Solo se pueden activar afiliados en estado PENDIENTE. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoAfiliado.ACTIVO;
    }

    public void darDeBaja(String motivo) {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException("El afiliado ya esta de baja");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de baja es obligatorio");
        }
        
        this.estado = EstadoAfiliado.BAJA;
        this.fechaBaja = LocalDateTime.now();
        this.motivoBaja = motivo;
    }

    public void reactivar() {
        if (this.estado != EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException(
                "Solo se pueden reactivar afiliados en estado BAJA. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoAfiliado.ACTIVO;
        this.fechaBaja = null;
        this.motivoBaja = null;
    }

    public void actualizarContacto(String email, String telefono) {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException(
                "No se pueden actualizar datos de un afiliado de baja"
            );
        }
        if (email != null) {
            this.email = email;
        }
        if (telefono != null) {
            this.telefono = telefono;
        }
    }

    public boolean estaActivo() {
        return this.estado == EstadoAfiliado.ACTIVO;
    }

    public int calcularEdad() {
        return LocalDate.now().getYear() - this.fechaNacimiento.getYear();
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" ").append(primerApellido);
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            sb.append(" ").append(segundoApellido);
        }
        return sb.toString();
    }

    private static void validarDatosObligatorios(
            Documento documento,
            String nombre,
            String primerApellido,
            LocalDate fechaNacimiento,
            String codigoEmpresa) {

        if (documento == null) {
            throw new IllegalArgumentException("El documento es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (primerApellido == null || primerApellido.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer apellido es obligatorio");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
            throw new IllegalArgumentException("El codigo de empresa es obligatorio");
        }
    }

    private static void validarEdadMinima(LocalDate fechaNacimiento) {
        int edad = LocalDate.now().getYear() - fechaNacimiento.getYear();
        if (edad < 18) {
            throw new IllegalArgumentException("El afiliado debe ser mayor de edad");
        }
    }
}
