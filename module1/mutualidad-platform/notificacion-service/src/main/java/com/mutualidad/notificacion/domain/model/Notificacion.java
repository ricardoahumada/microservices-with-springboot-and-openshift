package com.mutualidad.notificacion.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", indexes = {
    @Index(name = "idx_notificacion_afiliado", columnList = "afiliado_id"),
    @Index(name = "idx_notificacion_estado", columnList = "estado")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Destinatario destinatario;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200)
    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @NotBlank(message = "El contenido es obligatorio")
    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false)
    private Canal canal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Column(name = "intentos_envio")
    @Builder.Default
    private Integer intentosEnvio = 0;

    @Column(name = "error_mensaje", length = 500)
    private String errorMensaje;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void marcarComoEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
        this.intentosEnvio++;
    }

    public void marcarComoEntregada() {
        this.estado = EstadoNotificacion.ENTREGADA;
        this.fechaEntrega = LocalDateTime.now();
    }

    public void marcarComoLeida() {
        this.estado = EstadoNotificacion.LEIDA;
        this.fechaLectura = LocalDateTime.now();
    }

    public void marcarComoFallida(String error) {
        this.estado = EstadoNotificacion.FALLIDA;
        this.errorMensaje = error;
        this.intentosEnvio++;
    }

    public boolean puedeReintentar() {
        return this.estado == EstadoNotificacion.FALLIDA && this.intentosEnvio < 3;
    }
}
