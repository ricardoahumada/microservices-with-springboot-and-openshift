# Patrones de Relacioón entre Bounded Contexts

En **Domain-Driven Design (DDD)**, los **Bounded Contexts** (Contextos Delimitados) rara vez existen de forma aislada. Para coordinarlos, se definen **patrones de relación** que describen cómo colaboran entre sí. A continuación, te resumo los **principales tipos de relaciones** y te doy un **ejemplo en pseudocódigo Java** para cada uno.


### 1. **Customer/Supplier (Cliente/Proveedor)**
- **Descripción**: El contexto cliente depende del proveedor, pero tiene cierta influencia (por ejemplo, por prioridad del negocio) para negociar cambios en la interfaz.
- **Objetivo**: Colaboración activa para satisfacer necesidades del cliente.
- **Papel del cliente**: Puede hacer peticiones de mejora.

```java
// Contexto Proveedor: AFILIADO
public class AfiliadoService {
    public Afiliado getAfiliadoConBeneficios(String id) {
        // Devuelve datos específicos solicitados por el cliente
        return new Afiliado(id, "activo", List.of("seguro_medico"));
    }
}

// Contexto Cliente: BENEFICIO
public class BeneficioService {
    private AfiliadoService afiliadoService; // inyectado

    public void procesarBeneficio(String afiliadoId) {
        Afiliado afiliado = afiliadoService.getAfiliadoConBeneficios(afiliadoId);
        // Lógica de beneficio usando los datos del proveedor
    }
}
```

> *El proveedor adapta su API para facilitar el trabajo del cliente.*


### 2. **Conformist (Conformista)**
- **Descripción**: El contexto downstream acepta el modelo del upstream tal como está, sin pedir cambios.
- **Objetivo**: Reducir esfuerzo de integración a costa de aceptar un modelo externo.

```java
// Contexto Upstream: AFILIADO
public class AfiliadoDTO {
    public String id;
    public String estado;
}

// Contexto Downstream: VALIDACION
public class ValidacionService {
    public boolean esAfiliadoValido(AfiliadoDTO afiliado) {
        // Usa directamente el modelo del upstream
        return "activo".equals(afiliado.estado);
    }
}
```

> *El downstream "se conforma" con el modelo del upstream, aunque no sea ideal para su dominio.*


### 3. **Anti-Corruption Layer (ACL) – Capa Anticorrupción**
- **Descripción**: El downstream no quiere adoptar el modelo del upstream; en su lugar, traduce ese modelo a uno coherente con su propio dominio.
- **Objetivo**: Proteger la integridad del modelo propio.

```java
// Dominio externo (Upstream)
class AfiliadoExterno {
    String id;
    String estado;
}

// Dominio interno (Downstream)
class AfiliadoInterno {
    String id;
    boolean estaActivo() { return /* lógica interna */; }
}

// ACL: traductor
class AfiliadoTranslator {
    public AfiliadoInterno toInterno(AfiliadoExterno externo) {
        AfiliadoInterno interno = new AfiliadoInterno();
        interno.id = externo.id;
        // Traduce estado -> modelo interno
        // Ej: usa reglas específicas del dominio
        return interno;
    }
}

// Servicio downstream
public class ValidacionService {
    private AfiliadoTranslator translator;

    public boolean validar(AfiliadoExterno externo) {
        AfiliadoInterno interno = translator.toInterno(externo);
        return interno.estaActivo();
    }
}
```

>  *La ACL actúa como barrera que evita que el modelo externo "corrompa" el modelo interno.*


### 4. **Published Language (Lenguaje Publicado)**
- **Descripción**: Los contextos se comunican usando un **lenguaje formal y compartido**, como un esquema de mensajes, contrato de API, o formato estandarizado (JSON, XML, Protobuf).
- **Objetivo**: Reducir acoplamiento mediante un contrato explícito.

```java
// Contrato publicado (ej: en un módulo compartido o documentado)
public record BeneficioPublicado(String afiliadoId, String tipo, String fechaVencimiento) {}

// Contexto emisor: BENEFICIO
public class BeneficioPublisher {
    public BeneficioPublicado generarNotificacion(String afiliadoId) {
        return new BeneficioPublicado(afiliadoId, "seguro", "2025-12-31");
    }
}

// Contexto receptor: NOTIFICACION
public class NotificacionService {
    public void enviarNotificacion(BeneficioPublicado beneficio) {
        // Usa el modelo publicado sin conocer el dominio interno de BENEFICIO
        emailService.enviar(beneficio.afiliadoId(), "Tu beneficio vence pronto");
    }
}
```

> *Ambos contextos dependen del contrato, no del modelo interno del otro.*


### 5. **Partnership (Asociación)**
- **Descripción**: Dos contextos están fuertemente acoplados y deben evolucionar juntos.
- **Riesgo alto**: cambios en uno afectan directamente al otro.
- **Ejemplo omitido en pseudocódigo**, ya que se asemeja a Customer/Supplier, pero sin jerarquía: ambos equipos deben coordinarse estrechamente.


### 6. **Shared Kernel (Kernel Compartido)**
- **Descripción**: Dos contextos comparten un subconjunto de código o modelo.
- **Riesgo**: cambios en el kernel afectan a ambos.
- **Poco recomendado hoy en día** (mejor usar contratos explícitos).


### Resumen Rápido

| Relación                | ¿Quién se adapta?        | ¿Acoplamiento? | ¿Ejemplo típico?                |
|------------------------|--------------------------|----------------|----------------------------------|
| **Customer/Supplier**  | Proveedor al cliente     | Medio          | Backend de órdenes → Facturación |
| **Conformist**         | Downstream al upstream   | Alto (pasivo)  | Validación de datos externos     |
| **ACL**                | Downstream traduce       | Bajo           | Integración con sistema legado   |
| **Published Language** | Ambos usan contrato      | Muy bajo       | Eventos, APIs públicas           |
| **Partnership**        | Ambos se coordinan       | Muy alto       | Módulos interdependientes        |
| **Shared Kernel**      | Comparten código         | Alto           | Equipos pequeños y alineados     |

