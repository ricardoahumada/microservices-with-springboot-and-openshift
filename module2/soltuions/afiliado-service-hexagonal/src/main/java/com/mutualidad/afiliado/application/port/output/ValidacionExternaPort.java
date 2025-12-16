package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.model.Documento;

public interface ValidacionExternaPort {

    boolean validarDocumento(Documento documento);

    boolean verificarEstadoLaboral(String codigoEmpresa, String numeroDocumento);
}
