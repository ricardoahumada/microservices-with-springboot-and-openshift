# Commands CQRS - Beneficio Service

## Estructura de Commands

Los Commands representan intenciones de modificar el estado del sistema. Son objetos inmutables que encapsulan todos los datos necesarios para la operación.

## Commands Principales

### AsignarBeneficioCommand.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command para asignar un nuevo beneficio a un afiliado.
 * Inmutable y validable.
 */
@Value
@Builder
public class AsignarBeneficioCommand {

    @NotBlank(message = "El ID del afiliado es obligatorio")
    String afiliadoId;

    @NotNull(message = "El tipo de beneficio es obligatorio")
    TipoBeneficio tipoBeneficio;

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate fechaInicio;

    LocalDate fechaFin; // Opcional, null = indefinido

    @Positive(message = "El monto debe ser positivo")
    BigDecimal monto;

    String descripcion;

    @NotBlank(message = "El usuario que solicita es obligatorio")
    String solicitadoPor;

    String motivo;
}
```

### RevocarBeneficioCommand.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Command para revocar un beneficio existente.
 */
@Value
@Builder
public class RevocarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    @NotBlank(message = "El motivo de revocación es obligatorio")
    String motivo;

    LocalDate fechaEfectiva; // Si es null, es inmediato

    @NotBlank(message = "El usuario que revoca es obligatorio")
    String revocadoPor;
}
```

### ModificarBeneficioCommand.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command para modificar un beneficio existente.
 */
@Value
@Builder
public class ModificarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    BigDecimal nuevoMonto; // Null si no cambia

    LocalDate nuevaFechaFin; // Null si no cambia

    String nuevaDescripcion; // Null si no cambia

    @NotBlank(message = "El motivo de modificación es obligatorio")
    String motivo;

    @NotBlank(message = "El usuario que modifica es obligatorio")
    String modificadoPor;
}
```

### SuspenderBeneficioCommand.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Command para suspender temporalmente un beneficio.
 */
@Value
@Builder
public class SuspenderBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    @NotNull(message = "La fecha de inicio de suspensión es obligatoria")
    LocalDate fechaInicioSuspension;

    LocalDate fechaFinSuspension; // Si es null, suspensión indefinida

    @NotBlank(message = "El motivo de suspensión es obligatorio")
    String motivo;

    @NotBlank(message = "El usuario que suspende es obligatorio")
    String suspendidoPor;
}
```

### ReactivarBeneficioCommand.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

/**
 * Command para reactivar un beneficio suspendido.
 */
@Value
@Builder
public class ReactivarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    String motivo;

    @NotBlank(message = "El usuario que reactiva es obligatorio")
    String reactivadoPor;
}
```

## Enums de Soporte

### TipoBeneficio.java

```java
package com.mutualidad.beneficio.command.api;

/**
 * Tipos de beneficios disponibles en la mutualidad.
 */
public enum TipoBeneficio {
    
    SALUD("Cobertura de salud", true),
    SUBSIDIO_DESEMPLEO("Subsidio por desempleo", true),
    SUBSIDIO_INCAPACIDAD("Subsidio por incapacidad", true),
    DESCUENTO_FARMACIA("Descuento en farmacias", false),
    DESCUENTO_OPTICA("Descuento en ópticas", false),
    AYUDA_FAMILIAR("Ayuda por nacimiento/adopción", true),
    FORMACION("Ayuda para formación", true),
    JUBILACION_COMPLEMENTARIA("Complemento de jubilación", true);

    private final String descripcion;
    private final boolean requiereMonto;

    TipoBeneficio(String descripcion, boolean requiereMonto) {
        this.descripcion = descripcion;
        this.requiereMonto = requiereMonto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean requiereMonto() {
        return requiereMonto;
    }
}
```

## Command Result

### CommandResult.java

```java
package com.mutualidad.beneficio.command.api;

import lombok.Value;

import java.time.Instant;

/**
 * Resultado de la ejecución de un command.
 * Proporciona información sobre el resultado de la operación.
 */
@Value
public class CommandResult {

    String id;
    boolean success;
    String message;
    Instant timestamp;

    public static CommandResult success(String id) {
        return new CommandResult(id, true, "Operación completada exitosamente", Instant.now());
    }

    public static CommandResult success(String id, String message) {
        return new CommandResult(id, true, message, Instant.now());
    }

    public static CommandResult failure(String message) {
        return new CommandResult(null, false, message, Instant.now());
    }
}
```

## Principios de Diseño de Commands

| Principio | Descripción |
|-----------|-------------|
| **Inmutabilidad** | Usar `@Value` de Lombok garantiza inmutabilidad |
| **Auto-validación** | Anotaciones de Bean Validation incluidas |
| **Naming convention** | Verbo + Sustantivo + "Command" |
| **Trazabilidad** | Incluir quién y cuándo solicita la operación |
| **Intención clara** | El nombre describe exactamente qué hace |
| **Sin lógica** | Los commands son solo datos, sin comportamiento |

## Diagrama de Commands

```
┌─────────────────────────────────────────────────────────────────┐
│                       BENEFICIO COMMANDS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              AsignarBeneficioCommand                     │   │
│  │  - afiliadoId, tipoBeneficio, fechaInicio, monto        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ModificarBeneficioCommand                   │   │
│  │  - beneficioId, nuevoMonto, nuevaFechaFin, motivo       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              SuspenderBeneficioCommand                   │   │
│  │  - beneficioId, fechaInicioSuspension, motivo           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ReactivarBeneficioCommand                   │   │
│  │  - beneficioId, motivo                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              RevocarBeneficioCommand                     │   │
│  │  - beneficioId, motivo, fechaEfectiva                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```
