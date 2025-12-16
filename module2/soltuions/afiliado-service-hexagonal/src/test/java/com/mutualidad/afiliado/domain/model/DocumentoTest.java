package com.mutualidad.afiliado.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Documento Value Object Tests")
class DocumentoTest {

    @Nested
    @DisplayName("Creacion de Documento")
    class CreacionTests {

        @Test
        @DisplayName("Debe crear DNI valido")
        void debeCrearDniValido() {
            Documento documento = new Documento(TipoDocumento.DNI, "12345678Z");
            
            assertThat(documento.getTipo()).isEqualTo(TipoDocumento.DNI);
            assertThat(documento.getNumero()).isEqualTo("12345678Z");
        }

        @Test
        @DisplayName("Debe crear NIE valido")
        void debeCrearNieValido() {
            Documento documento = new Documento(TipoDocumento.NIE, "X1234567L");
            
            assertThat(documento.getTipo()).isEqualTo(TipoDocumento.NIE);
            assertThat(documento.getNumero()).isEqualTo("X1234567L");
        }

        @Test
        @DisplayName("Debe normalizar numero a mayusculas")
        void debeNormalizarAMayusculas() {
            Documento documento = new Documento(TipoDocumento.DNI, "12345678z");
            
            assertThat(documento.getNumero()).isEqualTo("12345678Z");
        }

        @Test
        @DisplayName("Debe fallar con formato DNI invalido")
        void debeFallarConFormatoDniInvalido() {
            assertThatThrownBy(() -> new Documento(TipoDocumento.DNI, "1234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato invalido");
        }

        @Test
        @DisplayName("Debe fallar con tipo null")
        void debeFallarConTipoNull() {
            assertThatThrownBy(() -> new Documento(null, "12345678Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de documento es obligatorio");
        }

        @Test
        @DisplayName("Debe fallar con numero vacio")
        void debeFallarConNumeroVacio() {
            assertThatThrownBy(() -> new Documento(TipoDocumento.DNI, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numero de documento es obligatorio");
        }
    }

    @Nested
    @DisplayName("Igualdad")
    class IgualdadTests {

        @Test
        @DisplayName("Documentos con mismos valores deben ser iguales")
        void documentosIguales() {
            Documento doc1 = new Documento(TipoDocumento.DNI, "12345678Z");
            Documento doc2 = new Documento(TipoDocumento.DNI, "12345678Z");
            
            assertThat(doc1).isEqualTo(doc2);
            assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
        }

        @Test
        @DisplayName("Documentos con distintos valores no deben ser iguales")
        void documentosDistintos() {
            Documento doc1 = new Documento(TipoDocumento.DNI, "12345678Z");
            Documento doc2 = new Documento(TipoDocumento.DNI, "87654321X");
            
            assertThat(doc1).isNotEqualTo(doc2);
        }
    }
}
