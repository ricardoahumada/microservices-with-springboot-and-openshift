package com.mutualidad.beneficio.command.domain;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Beneficio {

    private final String id;
    private final String afiliadoId;
    private final TipoBeneficio tipoBeneficio;
    private EstadoBeneficio estado;
    private final LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal monto;
    private String descripcion;
    private final String solicitadoPor;
    private final LocalDate fechaCreacion;

    private LocalDateTime fechaRevocacion;
    private String motivoRevocacion;
    private String revocadoPor;
    private LocalDateTime fechaSuspension;
    private String motivoSuspension;
    private String suspendidoPor;

    public void revocar(String motivo, LocalDate fechaEfectiva, String usuario) {
        if (!esRevocable()) {
            throw new IllegalStateException("El beneficio no puede ser revocado en estado: " + estado);
        }
        this.estado = EstadoBeneficio.REVOCADO;
        this.fechaFin = fechaEfectiva;
        this.motivoRevocacion = motivo;
        this.revocadoPor = usuario;
        this.fechaRevocacion = LocalDateTime.now();
    }

    public void suspender(String motivo, String usuario) {
        if (this.estado != EstadoBeneficio.ACTIVO) {
            throw new IllegalStateException("Solo se pueden suspender beneficios activos");
        }
        this.estado = EstadoBeneficio.SUSPENDIDO;
        this.motivoSuspension = motivo;
        this.suspendidoPor = usuario;
        this.fechaSuspension = LocalDateTime.now();
    }

    public void reactivar() {
        if (this.estado != EstadoBeneficio.SUSPENDIDO) {
            throw new IllegalStateException("Solo se pueden reactivar beneficios suspendidos");
        }
        this.estado = EstadoBeneficio.ACTIVO;
        this.motivoSuspension = null;
        this.suspendidoPor = null;
        this.fechaSuspension = null;
    }

    public void actualizarMonto(BigDecimal nuevoMonto) {
        if (nuevoMonto == null || nuevoMonto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        this.monto = nuevoMonto;
    }

    public void actualizarFechaFin(LocalDate nuevaFechaFin) {
        if (nuevaFechaFin != null && nuevaFechaFin.isBefore(this.fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior al inicio");
        }
        this.fechaFin = nuevaFechaFin;
    }

    public void actualizarDescripcion(String nuevaDescripcion) {
        this.descripcion = nuevaDescripcion;
    }

    public boolean esModificable() {
        return this.estado == EstadoBeneficio.ACTIVO || this.estado == EstadoBeneficio.SUSPENDIDO;
    }

    public boolean esRevocable() {
        return this.estado == EstadoBeneficio.ACTIVO || this.estado == EstadoBeneficio.SUSPENDIDO;
    }

    public boolean estaVigente(LocalDate fecha) {
        if (this.estado != EstadoBeneficio.ACTIVO) {
            return false;
        }
        boolean despuesDeInicio = !fecha.isBefore(this.fechaInicio);
        boolean antesDeFinOIndefinido = this.fechaFin == null || !fecha.isAfter(this.fechaFin);
        return despuesDeInicio && antesDeFinOIndefinido;
    }
}
