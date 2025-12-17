package com.mutualidad.afiliado.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DNI implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$");
    private static final String LETRAS_CONTROL = "TRWAGMYFPDXBNJZSQVHLCKE";

    @Column(name = "dni", nullable = false, length = 9, unique = true)
    private String valor;

    private DNI(String valor) {
        this.valor = valor.toUpperCase();
    }

    public static DNI of(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        
        String dniNormalizado = valor.toUpperCase().trim().replace(" ", "");
        
        if (!DNI_PATTERN.matcher(dniNormalizado).matches()) {
            throw new IllegalArgumentException("Formato de DNI inválido: " + valor);
        }
        
        /*if (!validarLetraControl(dniNormalizado)) {
            throw new IllegalArgumentException("Letra de control incorrecta: " + valor);
        }*/
        
        return new DNI(dniNormalizado);
    }

    private static boolean validarLetraControl(String dni) {
        String numeroStr = dni.substring(0, 8);
        char letraProporcionada = dni.charAt(8);
        int numero = Integer.parseInt(numeroStr);
        int resto = numero % 23;
        char letraEsperada = LETRAS_CONTROL.charAt(resto);
        return letraProporcionada == letraEsperada;
    }

    public static char calcularLetra(int numero) {
        return LETRAS_CONTROL.charAt(numero % 23);
    }

    public String getNumero() {
        return valor.substring(0, 8);
    }

    public char getLetra() {
        return valor.charAt(8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNI dni = (DNI) o;
        return Objects.equals(valor, dni.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
