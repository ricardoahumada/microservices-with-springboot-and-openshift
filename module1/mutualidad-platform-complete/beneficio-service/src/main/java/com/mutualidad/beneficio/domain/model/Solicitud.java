package com.mutualidad.beneficio.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes", indexes = {
    @Index(name = "idx_solicitud_afiliado", columnList = "afiliado_id"),
    @Index(name = "idx_solicitud_estado", columnList = "estado")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El ID del afiliado es obligatorio")
    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficio_id", nullable = false)
    private Beneficio beneficio;

    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "0.01")
    @Column(name = "monto_solicitado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoSolicitado;

    @Column(name = "monto_aprobado", precision = 10, scale = 2)
    private BigDecimal montoAprobado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    @Builder.Default
    private LocalDate fechaSolicitud = LocalDate.now();

    @Column(name = "fecha_resolucion")
    private LocalDate fechaResolucion;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "documentacion", length = 1000)
    private String documentacion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void aprobar(BigDecimal monto) {
        if (this.estado != EstadoSolicitud.PENDIENTE && this.estado != EstadoSolicitud.EN_REVISION) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes pendientes o en revision");
        }
        this.estado = EstadoSolicitud.APROBADA;
        this.montoAprobado = monto;
        this.fechaResolucion = LocalDate.now();
    }

    public void rechazar(String motivo) {
        if (this.estado != EstadoSolicitud.PENDIENTE && this.estado != EstadoSolicitud.EN_REVISION) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes pendientes o en revision");
        }
        this.estado = EstadoSolicitud.RECHAZADA;
        this.motivoRechazo = motivo;
        this.fechaResolucion = LocalDate.now();
    }

    public void marcarComoPagada() {
        if (this.estado != EstadoSolicitud.APROBADA) {
            throw new IllegalStateException("Solo se pueden pagar solicitudes aprobadas");
        }
        this.estado = EstadoSolicitud.PAGADA;
    }
}
