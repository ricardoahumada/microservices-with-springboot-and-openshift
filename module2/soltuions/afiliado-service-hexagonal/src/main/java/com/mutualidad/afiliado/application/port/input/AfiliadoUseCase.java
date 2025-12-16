package com.mutualidad.afiliado.application.port.input;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;

import java.util.Optional;

public interface AfiliadoUseCase {

    AfiliadoDTO registrarAfiliado(RegistrarAfiliadoCommand command);

    Optional<AfiliadoDTO> consultarPorDocumento(String tipoDocumento, String numeroDocumento);

    Optional<AfiliadoDTO> consultarPorId(String afiliadoId);

    void darDeBaja(String afiliadoId, String motivo);

    void reactivar(String afiliadoId);

    AfiliadoDTO actualizarContacto(String afiliadoId, String email, String telefono);
}
