package com.mutualidad.afiliado.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class DNITest {

    @Test
    @DisplayName("Debe crear DNI válido con letra correcta")
    void debeCrearDniValido() {
        DNI dni = DNI.of("12345678Z");
        
        assertThat(dni.getValor()).isEqualTo("12345678Z");
        assertThat(dni.getNumero()).isEqualTo("12345678");
        assertThat(dni.getLetra()).isEqualTo('Z');
    }

    @Test
    @DisplayName("Debe normalizar DNI en minúsculas")
    void debeNormalizarMinusculas() {
        DNI dni = DNI.of("12345678z");
        assertThat(dni.getValor()).isEqualTo("12345678Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000T", "12345678Z", "98765432M", "11111111H"})
    @DisplayName("Debe aceptar DNIs con letra correcta")
    void debeAceptarDnisValidos(String dniValido) {
        assertThatCode(() -> DNI.of(dniValido))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar DNI con letra incorrecta")
    void debeRechazarLetraIncorrecta() {
        assertThatThrownBy(() -> DNI.of("12345678A"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Letra de control incorrecta");
    }

    @Test
    @DisplayName("Debe rechazar DNI con formato inválido")
    void debeRechazarFormatoInvalido() {
        assertThatThrownBy(() -> DNI.of("1234567Z"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Formato de DNI inválido");
    }

    @Test
    @DisplayName("Dos DNIs iguales deben ser equals")
    void dosIgualesDebenSerEquals() {
        DNI dni1 = DNI.of("12345678Z");
        DNI dni2 = DNI.of("12345678Z");
        
        assertThat(dni1).isEqualTo(dni2);
        assertThat(dni1.hashCode()).isEqualTo(dni2.hashCode());
    }

    @Test
    @DisplayName("Debe calcular letra correctamente")
    void debeCalcularLetra() {
        assertThat(DNI.calcularLetra(0)).isEqualTo('T');
        assertThat(DNI.calcularLetra(12345678)).isEqualTo('Z');
    }
}
