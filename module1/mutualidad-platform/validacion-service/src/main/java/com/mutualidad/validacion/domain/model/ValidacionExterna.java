package com.mutualidad.validacion.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validaciones_externas", indexes = {
    @Index(name = "idx_validacion_afiliado", columnList = "afiliado_id"),
    @Index(name = "idx_validacion_resultado", columnList = "resultado")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacionExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El ID del afiliado es obligatorio")
    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    @NotNull(message = "El tipo de validacion es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoValidacion tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    @Builder.Default
    private ResultadoValidacion resultado = ResultadoValidacion.PENDIENTE;

    @Column(name = "proveedor_externo", length = 100)
    private String proveedorExterno;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "datos_enviados", columnDefinition = "TEXT")
    private String datosEnviados;

    @Column(name = "respuesta_proveedor", columnDefinition = "TEXT")
    private String respuestaProveedor;

    @Column(name = "puntuacion")
    private Integer puntuacion;

    @Column(name = "mensaje_resultado", length = 500)
    private String mensajeResultado;

    @Column(name = "fecha_solicitud", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void iniciarProceso(String proveedor, String referencia) {
        this.resultado = ResultadoValidacion.EN_PROCESO;
        this.proveedorExterno = proveedor;
        this.referenciaExterna = referencia;
    }

    public void aprobar(Integer puntuacion, String mensaje, String respuesta) {
        this.resultado = ResultadoValidacion.APROBADO;
        this.puntuacion = puntuacion;
        this.mensajeResultado = mensaje;
        this.respuestaProveedor = respuesta;
        this.fechaRespuesta = LocalDateTime.now();
        this.fechaExpiracion = LocalDateTime.now().plusYears(1);
    }

    public void rechazar(String mensaje, String respuesta) {
        this.resultado = ResultadoValidacion.RECHAZADO;
        this.mensajeResultado = mensaje;
        this.respuestaProveedor = respuesta;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public void registrarError(String mensaje) {
        this.resultado = ResultadoValidacion.ERROR;
        this.mensajeResultado = mensaje;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public boolean estaVigente() {
        return this.resultado == ResultadoValidacion.APROBADO 
                && this.fechaExpiracion != null 
                && this.fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
