package com.mutualidad.afiliado.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "afiliados", indexes = {
    @Index(name = "idx_afiliado_dni", columnList = "dni", unique = true),
    @Index(name = "idx_afiliado_email", columnList = "email"),
    @Index(name = "idx_afiliado_estado", columnList = "estado")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Afiliado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "dni", nullable = false, length = 9))
    private DNI dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 150)
    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Formato de teléfono inválido")
    @Column(name = "telefono", length = 15)
    private String telefono;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoAfiliado estado = EstadoAfiliado.PENDIENTE;

    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 500)
    private String motivoBaja;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Métodos de negocio
    public void activar() {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new IllegalStateException("No se puede activar un afiliado dado de baja");
        }
        this.estado = EstadoAfiliado.ACTIVO;
        this.fechaAlta = LocalDate.now();
    }

    public void darDeBaja(String motivo) {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new IllegalStateException("El afiliado ya está dado de baja");
        }
        this.estado = EstadoAfiliado.BAJA;
        this.fechaBaja = LocalDate.now();
        this.motivoBaja = motivo;
    }

    public boolean puedeRecibirBeneficios() {
        return this.estado == EstadoAfiliado.ACTIVO;
    }

    public String getNombreCompleto() {
        return String.format("%s %s", nombre, apellidos);
    }
}
