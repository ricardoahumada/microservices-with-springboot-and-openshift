package com.mutualidad.afiliado.infrastructure.adapter.input.rest;

import lombok.Data;

@Data
public class ActualizarContactoRequest {
    private String email;
    private String telefono;
}
