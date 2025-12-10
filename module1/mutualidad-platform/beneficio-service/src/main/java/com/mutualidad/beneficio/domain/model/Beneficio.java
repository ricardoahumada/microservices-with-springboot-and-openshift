package com.mutualidad.beneficio.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficios")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoBeneficio tipo;

    @NotNull(message = "El monto maximo es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    @Column(name = "monto_maximo", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoMaximo;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @Min(value = 0)
    @Column(name = "dias_carencia")
    @Builder.Default
    private Integer diasCarencia = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public boolean estaDisponible() {
        return Boolean.TRUE.equals(this.activo);
    }
}
