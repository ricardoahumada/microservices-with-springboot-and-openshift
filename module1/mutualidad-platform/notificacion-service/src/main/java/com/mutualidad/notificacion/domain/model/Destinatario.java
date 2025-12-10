package com.mutualidad.notificacion.domain.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destinatario {

    @NotBlank
    @Column(name = "afiliado_id")
    private String afiliadoId;

    @Column(name = "nombre_destinatario", length = 150)
    private String nombre;

    @Email
    @Column(name = "email_destinatario", length = 150)
    private String email;

    @Column(name = "telefono_destinatario", length = 20)
    private String telefono;

    public boolean puedeRecibirEmail() {
        return email != null && !email.isBlank();
    }

    public boolean puedeRecibirSms() {
        return telefono != null && !telefono.isBlank();
    }
}
