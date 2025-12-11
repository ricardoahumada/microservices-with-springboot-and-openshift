# Puerto de Entrada - Arquitectura Hexagonal

## AfiliadoUseCase.java

Este puerto define las operaciones que el dominio expone al mundo exterior.

```java
package com.mutualidad.afiliado.application.port.input;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;

import java.util.Optional;

/**
 * Puerto de entrada que define los casos de uso del servicio de afiliados.
 * Esta interfaz es implementada por el servicio de aplicación y consumida
 * por los adaptadores primarios (REST, GraphQL, CLI, etc.)
 */
public interface AfiliadoUseCase {

    /**
     * Registra un nuevo afiliado en el sistema.
     * 
     * @param command Datos del afiliado a registrar
     * @return DTO con los datos del afiliado registrado
     * @throws AfiliadoYaExisteException si el documento ya está registrado
     * @throws DocumentoInvalidoException si el documento no pasa validación externa
     */
    AfiliadoDTO registrarAfiliado(RegistrarAfiliadoCommand command);

    /**
     * Consulta un afiliado por su número de documento.
     * 
     * @param tipoDocumento Tipo de documento (DNI, NIE, PASAPORTE)
     * @param numeroDocumento Número del documento
     * @return Optional con el afiliado si existe
     */
    Optional<AfiliadoDTO> consultarPorDocumento(String tipoDocumento, String numeroDocumento);

    /**
     * Consulta un afiliado por su ID interno.
     * 
     * @param afiliadoId ID único del afiliado
     * @return Optional con el afiliado si existe
     */
    Optional<AfiliadoDTO> consultarPorId(String afiliadoId);

    /**
     * Da de baja a un afiliado.
     * 
     * @param afiliadoId ID del afiliado
     * @param motivo Motivo de la baja
     * @throws AfiliadoNoEncontradoException si el afiliado no existe
     * @throws EstadoInvalidoException si el afiliado ya está de baja
     */
    void darDeBaja(String afiliadoId, String motivo);

    /**
     * Reactiva un afiliado dado de baja.
     * 
     * @param afiliadoId ID del afiliado
     * @throws AfiliadoNoEncontradoException si el afiliado no existe
     * @throws EstadoInvalidoException si el afiliado no está de baja
     */
    void reactivar(String afiliadoId);

    /**
     * Actualiza los datos de contacto de un afiliado.
     * 
     * @param afiliadoId ID del afiliado
     * @param email Nuevo email (puede ser null para no actualizar)
     * @param telefono Nuevo teléfono (puede ser null para no actualizar)
     * @return DTO actualizado
     */
    AfiliadoDTO actualizarContacto(String afiliadoId, String email, String telefono);
}
```

## RegistrarAfiliadoCommand.java

Command que encapsula los datos necesarios para registrar un afiliado:

```java
package com.mutualidad.afiliado.application.dto;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDate;

/**
 * Command para el registro de un nuevo afiliado.
 * Inmutable y validable.
 */
@Value
@Builder
public class RegistrarAfiliadoCommand {

    @NotBlank(message = "El tipo de documento es obligatorio")
    String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    String numeroDocumento;

    @NotBlank(message = "El nombre es obligatorio")
    String nombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    String primerApellido;

    String segundoApellido;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    LocalDate fechaNacimiento;

    @Email(message = "El email debe tener un formato válido")
    String email;

    String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    String direccion;

    String codigoPostal;

    String provincia;

    @NotBlank(message = "El código de empresa es obligatorio")
    String codigoEmpresa;
}
```

## AfiliadoDTO.java

DTO de respuesta:

```java
package com.mutualidad.afiliado.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para operaciones de afiliado.
 */
@Value
@Builder
public class AfiliadoDTO {

    String id;
    String tipoDocumento;
    String numeroDocumento;
    String nombreCompleto;
    LocalDate fechaNacimiento;
    String email;
    String telefono;
    String direccion;
    String estado;
    LocalDateTime fechaAlta;
    LocalDateTime fechaBaja;
    String motivoBaja;
    String codigoEmpresa;
}
```

## Notas de Diseño

1. **Interfaz pura**: No hay dependencias de Spring ni otros frameworks
2. **DTOs inmutables**: Uso de `@Value` de Lombok para garantizar inmutabilidad
3. **Validación declarativa**: Uso de Bean Validation en los commands
4. **Documentación clara**: Javadoc que describe comportamiento y excepciones
5. **Optional para consultas**: Evita null returns en búsquedas
