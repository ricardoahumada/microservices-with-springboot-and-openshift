package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.domain.model.Afiliado;
import org.springframework.stereotype.Component;

@Component
public class AfiliadoMapper {

    public AfiliadoDTO toDTO(Afiliado afiliado) {
        return AfiliadoDTO.builder()
            .id(afiliado.getId())
            .tipoDocumento(afiliado.getDocumento().getTipo().name())
            .numeroDocumento(afiliado.getDocumento().getNumero())
            .nombreCompleto(afiliado.getNombreCompleto())
            .fechaNacimiento(afiliado.getFechaNacimiento())
            .email(afiliado.getEmail())
            .telefono(afiliado.getTelefono())
            .direccion(afiliado.getDireccion())
            .estado(afiliado.getEstado().name())
            .fechaAlta(afiliado.getFechaAlta())
            .fechaBaja(afiliado.getFechaBaja())
            .motivoBaja(afiliado.getMotivoBaja())
            .codigoEmpresa(afiliado.getCodigoEmpresa())
            .build();
    }
}
