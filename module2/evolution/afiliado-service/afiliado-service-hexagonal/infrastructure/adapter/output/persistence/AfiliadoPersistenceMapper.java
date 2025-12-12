package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import com.mutualidad.afiliado.domain.model.TipoDocumento;
import org.springframework.stereotype.Component;

@Component
public class AfiliadoPersistenceMapper {

    public AfiliadoEntity toEntity(Afiliado afiliado) {
        return AfiliadoEntity.builder()
            .id(afiliado.getId())
            .tipoDocumento(afiliado.getDocumento().getTipo().name())
            .numeroDocumento(afiliado.getDocumento().getNumero())
            .nombre(afiliado.getNombre())
            .primerApellido(afiliado.getPrimerApellido())
            .segundoApellido(afiliado.getSegundoApellido())
            .fechaNacimiento(afiliado.getFechaNacimiento())
            .email(afiliado.getEmail())
            .telefono(afiliado.getTelefono())
            .direccion(afiliado.getDireccion())
            .codigoPostal(afiliado.getCodigoPostal())
            .provincia(afiliado.getProvincia())
            .estado(afiliado.getEstado().name())
            .fechaAlta(afiliado.getFechaAlta())
            .fechaBaja(afiliado.getFechaBaja())
            .motivoBaja(afiliado.getMotivoBaja())
            .codigoEmpresa(afiliado.getCodigoEmpresa())
            .build();
    }

    public Afiliado toDomain(AfiliadoEntity entity) {
        Documento documento = new Documento(
            TipoDocumento.valueOf(entity.getTipoDocumento()),
            entity.getNumeroDocumento()
        );

        return Afiliado.reconstitute(
            entity.getId(),
            documento,
            entity.getNombre(),
            entity.getPrimerApellido(),
            entity.getSegundoApellido(),
            entity.getFechaNacimiento(),
            entity.getEmail(),
            entity.getTelefono(),
            entity.getDireccion(),
            entity.getCodigoPostal(),
            entity.getProvincia(),
            EstadoAfiliado.valueOf(entity.getEstado()),
            entity.getFechaAlta(),
            entity.getFechaBaja(),
            entity.getMotivoBaja(),
            entity.getCodigoEmpresa()
        );
    }
}
