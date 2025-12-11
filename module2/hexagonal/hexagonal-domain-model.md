# Modelo de Dominio - Arquitectura Hexagonal

## Afiliado.java - Entidad Principal

```java
package com.mutualidad.afiliado.domain.model;

import com.mutualidad.afiliado.domain.exception.EstadoInvalidoException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa un afiliado de la mutualidad.
 * Contiene toda la lógica de negocio relacionada con el afiliado.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Afiliado {

    private final String id;
    private final Documento documento;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private String direccion;
    private String codigoPostal;
    private String provincia;
    private EstadoAfiliado estado;
    private final LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private String motivoBaja;
    private final String codigoEmpresa;

    /**
     * Factory method para crear un nuevo afiliado.
     * El afiliado se crea en estado PENDIENTE hasta que se valide.
     */
    public static Afiliado crear(
            Documento documento,
            String nombre,
            String primerApellido,
            String segundoApellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String direccion,
            String codigoPostal,
            String provincia,
            String codigoEmpresa) {

        validarDatosObligatorios(documento, nombre, primerApellido, fechaNacimiento, codigoEmpresa);
        validarEdadMinima(fechaNacimiento);

        return new Afiliado(
            UUID.randomUUID().toString(),
            documento,
            nombre,
            primerApellido,
            segundoApellido,
            fechaNacimiento,
            email,
            telefono,
            direccion,
            codigoPostal,
            provincia,
            EstadoAfiliado.PENDIENTE,
            LocalDateTime.now(),
            null,
            null,
            codigoEmpresa
        );
    }

    /**
     * Factory method para reconstituir un afiliado desde persistencia.
     */
    public static Afiliado reconstitute(
            String id,
            Documento documento,
            String nombre,
            String primerApellido,
            String segundoApellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String direccion,
            String codigoPostal,
            String provincia,
            EstadoAfiliado estado,
            LocalDateTime fechaAlta,
            LocalDateTime fechaBaja,
            String motivoBaja,
            String codigoEmpresa) {

        return new Afiliado(
            id, documento, nombre, primerApellido, segundoApellido,
            fechaNacimiento, email, telefono, direccion, codigoPostal,
            provincia, estado, fechaAlta, fechaBaja, motivoBaja, codigoEmpresa
        );
    }

    /**
     * Activa el afiliado tras validación exitosa.
     */
    public void activar() {
        if (this.estado != EstadoAfiliado.PENDIENTE) {
            throw new EstadoInvalidoException(
                "Solo se pueden activar afiliados en estado PENDIENTE. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoAfiliado.ACTIVO;
    }

    /**
     * Da de baja al afiliado con un motivo.
     */
    public void darDeBaja(String motivo) {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException("El afiliado ya está de baja");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de baja es obligatorio");
        }
        
        this.estado = EstadoAfiliado.BAJA;
        this.fechaBaja = LocalDateTime.now();
        this.motivoBaja = motivo;
    }

    /**
     * Reactiva un afiliado que estaba de baja.
     */
    public void reactivar() {
        if (this.estado != EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException(
                "Solo se pueden reactivar afiliados en estado BAJA. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoAfiliado.ACTIVO;
        this.fechaBaja = null;
        this.motivoBaja = null;
    }

    /**
     * Actualiza los datos de contacto.
     */
    public void actualizarContacto(String email, String telefono) {
        if (this.estado == EstadoAfiliado.BAJA) {
            throw new EstadoInvalidoException(
                "No se pueden actualizar datos de un afiliado de baja"
            );
        }
        if (email != null) {
            this.email = email;
        }
        if (telefono != null) {
            this.telefono = telefono;
        }
    }

    /**
     * Verifica si el afiliado está activo.
     */
    public boolean estaActivo() {
        return this.estado == EstadoAfiliado.ACTIVO;
    }

    /**
     * Calcula la edad del afiliado.
     */
    public int calcularEdad() {
        return LocalDate.now().getYear() - this.fechaNacimiento.getYear();
    }

    /**
     * Obtiene el nombre completo del afiliado.
     */
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" ").append(primerApellido);
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            sb.append(" ").append(segundoApellido);
        }
        return sb.toString();
    }

    // Validaciones privadas
    private static void validarDatosObligatorios(
            Documento documento,
            String nombre,
            String primerApellido,
            LocalDate fechaNacimiento,
            String codigoEmpresa) {

        if (documento == null) {
            throw new IllegalArgumentException("El documento es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (primerApellido == null || primerApellido.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer apellido es obligatorio");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de empresa es obligatorio");
        }
    }

    private static void validarEdadMinima(LocalDate fechaNacimiento) {
        int edad = LocalDate.now().getYear() - fechaNacimiento.getYear();
        if (edad < 18) {
            throw new IllegalArgumentException("El afiliado debe ser mayor de edad");
        }
    }
}
```

## Documento.java - Value Object

```java
package com.mutualidad.afiliado.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * Value Object que representa el documento de identidad.
 * Inmutable y auto-validable.
 */
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
            throw new IllegalArgumentException("El número de documento es obligatorio");
        }

        String numeroNormalizado = numero.toUpperCase().trim();
        validarFormato(tipo, numeroNormalizado);

        this.tipo = tipo;
        this.numero = numeroNormalizado;
    }

    /**
     * Factory method para crear desde strings.
     */
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
                String.format("Formato inválido para %s: %s", tipo, numero)
            );
        }
    }

    /**
     * Valida la letra del DNI usando el algoritmo oficial.
     */
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
```

## EstadoAfiliado.java - Enum con Transiciones

```java
package com.mutualidad.afiliado.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados posibles del afiliado con transiciones válidas.
 */
public enum EstadoAfiliado {

    PENDIENTE {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO, RECHAZADO);
        }
    },
    ACTIVO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(BAJA, SUSPENDIDO);
        }
    },
    SUSPENDIDO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO, BAJA);
        }
    },
    BAJA {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.of(ACTIVO); // Reactivación
        }
    },
    RECHAZADO {
        @Override
        public Set<EstadoAfiliado> transicionesValidas() {
            return EnumSet.noneOf(EstadoAfiliado.class); // Estado final
        }
    };

    /**
     * Retorna los estados a los que se puede transicionar desde el estado actual.
     */
    public abstract Set<EstadoAfiliado> transicionesValidas();

    /**
     * Verifica si una transición es válida.
     */
    public boolean puedeTransicionarA(EstadoAfiliado nuevoEstado) {
        return transicionesValidas().contains(nuevoEstado);
    }
}
```

## TipoDocumento.java - Enum

```java
package com.mutualidad.afiliado.domain.model;

/**
 * Tipos de documento de identidad soportados.
 */
public enum TipoDocumento {
    DNI("Documento Nacional de Identidad"),
    NIE("Número de Identidad de Extranjero"),
    PASAPORTE("Pasaporte");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
```

## Excepciones de Dominio

```java
package com.mutualidad.afiliado.domain.exception;

/**
 * Excepción base para errores de dominio del afiliado.
 */
public class AfiliadoException extends RuntimeException {
    public AfiliadoException(String message) {
        super(message);
    }
}

/**
 * Se lanza cuando se intenta registrar un afiliado que ya existe.
 */
public class AfiliadoYaExisteException extends AfiliadoException {
    public AfiliadoYaExisteException(String documento) {
        super("Ya existe un afiliado con el documento: " + documento);
    }
}

/**
 * Se lanza cuando no se encuentra un afiliado.
 */
public class AfiliadoNoEncontradoException extends AfiliadoException {
    public AfiliadoNoEncontradoException(String id) {
        super("No se encontró el afiliado con ID: " + id);
    }
}

/**
 * Se lanza cuando una transición de estado no es válida.
 */
public class EstadoInvalidoException extends AfiliadoException {
    public EstadoInvalidoException(String message) {
        super(message);
    }
}

/**
 * Se lanza cuando un documento no pasa la validación externa.
 */
public class DocumentoInvalidoException extends AfiliadoException {
    public DocumentoInvalidoException(String documento) {
        super("El documento no pudo ser validado: " + documento);
    }
}
```

## Notas de Diseño

1. **Sin dependencias de frameworks**: El dominio es Java puro
2. **Invariantes protegidos**: Las validaciones se ejecutan en la creación
3. **Inmutabilidad donde aplica**: Value Objects completamente inmutables
4. **Factory Methods**: Control sobre la creación de entidades
5. **Comportamiento rico**: La lógica de negocio vive en el dominio
6. **Transiciones explícitas**: El enum define qué cambios de estado son válidos
