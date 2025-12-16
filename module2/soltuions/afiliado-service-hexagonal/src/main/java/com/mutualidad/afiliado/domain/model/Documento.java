package com.mutualidad.afiliado.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@ToString
public class Documento {

    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ][0-9]{7}[A-Z]$");
    private static final Pattern PASAPORTE_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{6}$");

    private final TipoDocumento tipo;
    private final String numero;

    public Documento(TipoDocumento tipo, String numero) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        if (numero == null || numero.trim().isEmpty()) {
            throw new IllegalArgumentException("El numero de documento es obligatorio");
        }

        String numeroNormalizado = numero.toUpperCase().trim();
        validarFormato(tipo, numeroNormalizado);

        this.tipo = tipo;
        this.numero = numeroNormalizado;
    }

    public static Documento of(String tipo, String numero) {
        TipoDocumento tipoDocumento = TipoDocumento.valueOf(tipo.toUpperCase());
        return new Documento(tipoDocumento, numero);
    }

    private void validarFormato(TipoDocumento tipo, String numero) {
        boolean valido = switch (tipo) {
            case DNI -> DNI_PATTERN.matcher(numero).matches();
            case NIE -> NIE_PATTERN.matcher(numero).matches();
            case PASAPORTE -> PASAPORTE_PATTERN.matcher(numero).matches();
        };

        if (!valido) {
            throw new IllegalArgumentException(
                String.format("Formato invalido para %s: %s", tipo, numero)
            );
        }
    }

    public boolean validarLetraDNI() {
        if (tipo != TipoDocumento.DNI) {
            return true;
        }

        String letras = "TRWAGMYFPDXBNJZSQVHLCKE";
        int numeroParte = Integer.parseInt(numero.substring(0, 8));
        char letraCalculada = letras.charAt(numeroParte % 23);
        char letraDocumento = numero.charAt(8);

        return letraCalculada == letraDocumento;
    }
}
