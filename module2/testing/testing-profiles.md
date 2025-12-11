# Testing y Perfiles Spring - Microservicios

## Pruebas Unitarias del Dominio

### AfiliadoTest.java

```java
package com.mutualidad.afiliado.domain.model;

import com.mutualidad.afiliado.domain.exception.EstadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Afiliado Domain Tests")
class AfiliadoTest {

    private static final Documento DOCUMENTO_VALIDO = 
        new Documento(TipoDocumento.DNI, "12345678Z");

    @Nested
    @DisplayName("Creación de Afiliado")
    class CreacionTests {

        @Test
        @DisplayName("Debe crear afiliado en estado PENDIENTE")
        void debeCrearAfiliadoEnEstadoPendiente() {
            // Given
            LocalDate fechaNacimiento = LocalDate.of(1990, 5, 15);

            // When
            Afiliado afiliado = Afiliado.crear(
                DOCUMENTO_VALIDO,
                "Juan",
                "García",
                "López",
                fechaNacimiento,
                "juan@email.com",
                "600123456",
                "Calle Mayor 1",
                "28001",
                "Madrid",
                "EMP001"
            );

            // Then
            assertThat(afiliado.getId()).isNotNull();
            assertThat(afiliado.getEstado()).isEqualTo(EstadoAfiliado.PENDIENTE);
            assertThat(afiliado.getFechaAlta()).isNotNull();
            assertThat(afiliado.getNombreCompleto()).isEqualTo("Juan García López");
        }

        @Test
        @DisplayName("Debe rechazar afiliado menor de edad")
        void debeRechazarMenorDeEdad() {
            // Given
            LocalDate fechaNacimientoMenor = LocalDate.now().minusYears(17);

            // When/Then
            assertThatThrownBy(() -> Afiliado.crear(
                DOCUMENTO_VALIDO,
                "Juan",
                "García",
                null,
                fechaNacimientoMenor,
                null, null, "Dirección", null, null,
                "EMP001"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mayor de edad");
        }

        @Test
        @DisplayName("Debe validar datos obligatorios")
        void debeValidarDatosObligatorios() {
            // Given
            LocalDate fechaNacimiento = LocalDate.of(1990, 5, 15);

            // When/Then - Sin nombre
            assertThatThrownBy(() -> Afiliado.crear(
                DOCUMENTO_VALIDO, null, "García", null,
                fechaNacimiento, null, null, "Dir", null, null, "EMP001"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nombre");

            // When/Then - Sin documento
            assertThatThrownBy(() -> Afiliado.crear(
                null, "Juan", "García", null,
                fechaNacimiento, null, null, "Dir", null, null, "EMP001"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("documento");
        }
    }

    @Nested
    @DisplayName("Transiciones de Estado")
    class TransicionesEstadoTests {

        @Test
        @DisplayName("Debe activar afiliado pendiente")
        void debeActivarAfiliadoPendiente() {
            // Given
            Afiliado afiliado = crearAfiliadoPendiente();
            assertThat(afiliado.getEstado()).isEqualTo(EstadoAfiliado.PENDIENTE);

            // When
            afiliado.activar();

            // Then
            assertThat(afiliado.getEstado()).isEqualTo(EstadoAfiliado.ACTIVO);
        }

        @Test
        @DisplayName("No debe activar afiliado ya activo")
        void noDebeActivarAfiliadoYaActivo() {
            // Given
            Afiliado afiliado = crearAfiliadoPendiente();
            afiliado.activar();

            // When/Then
            assertThatThrownBy(() -> afiliado.activar())
                .isInstanceOf(EstadoInvalidoException.class);
        }

        @Test
        @DisplayName("Debe dar de baja con motivo")
        void debeDarDeBajaConMotivo() {
            // Given
            Afiliado afiliado = crearAfiliadoActivo();
            String motivo = "Cambio de mutualidad";

            // When
            afiliado.darDeBaja(motivo);

            // Then
            assertThat(afiliado.getEstado()).isEqualTo(EstadoAfiliado.BAJA);
            assertThat(afiliado.getMotivoBaja()).isEqualTo(motivo);
            assertThat(afiliado.getFechaBaja()).isNotNull();
        }

        @Test
        @DisplayName("Debe requerir motivo para baja")
        void debeRequerirMotivoParaBaja() {
            // Given
            Afiliado afiliado = crearAfiliadoActivo();

            // When/Then
            assertThatThrownBy(() -> afiliado.darDeBaja(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");

            assertThatThrownBy(() -> afiliado.darDeBaja("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("Debe reactivar afiliado de baja")
        void debeReactivarAfiliadoDeBaja() {
            // Given
            Afiliado afiliado = crearAfiliadoActivo();
            afiliado.darDeBaja("Motivo temporal");

            // When
            afiliado.reactivar();

            // Then
            assertThat(afiliado.getEstado()).isEqualTo(EstadoAfiliado.ACTIVO);
            assertThat(afiliado.getFechaBaja()).isNull();
            assertThat(afiliado.getMotivoBaja()).isNull();
        }
    }

    // Métodos auxiliares
    private Afiliado crearAfiliadoPendiente() {
        return Afiliado.crear(
            DOCUMENTO_VALIDO, "Juan", "García", "López",
            LocalDate.of(1990, 5, 15),
            "juan@email.com", "600123456",
            "Calle Mayor 1", "28001", "Madrid", "EMP001"
        );
    }

    private Afiliado crearAfiliadoActivo() {
        Afiliado afiliado = crearAfiliadoPendiente();
        afiliado.activar();
        return afiliado;
    }
}
```

### DocumentoTest.java

```java
package com.mutualidad.afiliado.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Documento Value Object Tests")
class DocumentoTest {

    @Test
    @DisplayName("Debe crear documento DNI válido")
    void debeCrearDocumentoDNIValido() {
        // When
        Documento doc = new Documento(TipoDocumento.DNI, "12345678z");

        // Then
        assertThat(doc.getTipo()).isEqualTo(TipoDocumento.DNI);
        assertThat(doc.getNumero()).isEqualTo("12345678Z"); // Normalizado
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567Z", "123456789Z", "12345678", "ABCDEFGHZ"})
    @DisplayName("Debe rechazar DNI con formato inválido")
    void debeRechazarDNIFormatoInvalido(String numero) {
        assertThatThrownBy(() -> new Documento(TipoDocumento.DNI, numero))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Formato inválido");
    }

    @ParameterizedTest
    @CsvSource({
        "NIE, X1234567L",
        "NIE, Y9876543M",
        "NIE, Z0000000N"
    })
    @DisplayName("Debe crear documento NIE válido")
    void debeCrearNIEValido(TipoDocumento tipo, String numero) {
        Documento doc = new Documento(tipo, numero);
        assertThat(doc.getNumero()).isEqualTo(numero.toUpperCase());
    }

    @Test
    @DisplayName("Documentos iguales deben ser equals")
    void documentosIgualesDebenSerEquals() {
        Documento doc1 = new Documento(TipoDocumento.DNI, "12345678Z");
        Documento doc2 = new Documento(TipoDocumento.DNI, "12345678z");

        assertThat(doc1).isEqualTo(doc2);
        assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
    }
}
```

## Pruebas de Integración

### AfiliadoApplicationServiceIT.java

```java
package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;
import com.mutualidad.afiliado.application.port.input.AfiliadoUseCase;
import com.mutualidad.afiliado.domain.exception.AfiliadoYaExisteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Afiliado Application Service Integration Tests")
class AfiliadoApplicationServiceIT {

    @Autowired
    private AfiliadoUseCase afiliadoUseCase;

    private RegistrarAfiliadoCommand commandValido;

    @BeforeEach
    void setUp() {
        commandValido = RegistrarAfiliadoCommand.builder()
            .tipoDocumento("DNI")
            .numeroDocumento("12345678Z")
            .nombre("Juan")
            .primerApellido("García")
            .segundoApellido("López")
            .fechaNacimiento(LocalDate.of(1990, 5, 15))
            .email("juan@email.com")
            .telefono("600123456")
            .direccion("Calle Mayor 1")
            .codigoPostal("28001")
            .provincia("Madrid")
            .codigoEmpresa("EMP001")
            .build();
    }

    @Test
    @DisplayName("Debe registrar afiliado completo")
    void debeRegistrarAfiliadoCompleto() {
        // When
        AfiliadoDTO resultado = afiliadoUseCase.registrarAfiliado(commandValido);

        // Then
        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getNombreCompleto()).isEqualTo("Juan García López");
        assertThat(resultado.getEstado()).isIn("PENDIENTE", "ACTIVO");
    }

    @Test
    @DisplayName("Debe consultar afiliado por documento")
    void debeConsultarPorDocumento() {
        // Given
        afiliadoUseCase.registrarAfiliado(commandValido);

        // When
        Optional<AfiliadoDTO> resultado = afiliadoUseCase.consultarPorDocumento(
            "DNI", "12345678Z"
        );

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNumeroDocumento()).isEqualTo("12345678Z");
    }

    @Test
    @DisplayName("Debe rechazar afiliado duplicado")
    void debeRechazarAfiliadoDuplicado() {
        // Given
        afiliadoUseCase.registrarAfiliado(commandValido);

        // When/Then
        assertThatThrownBy(() -> afiliadoUseCase.registrarAfiliado(commandValido))
            .isInstanceOf(AfiliadoYaExisteException.class);
    }

    @Test
    @DisplayName("Debe dar de baja y reactivar afiliado")
    void debeDarDeBajaYReactivar() {
        // Given
        AfiliadoDTO afiliado = afiliadoUseCase.registrarAfiliado(commandValido);
        String afiliadoId = afiliado.getId();

        // When - Dar de baja
        afiliadoUseCase.darDeBaja(afiliadoId, "Cambio de empresa");

        // Then
        Optional<AfiliadoDTO> bajado = afiliadoUseCase.consultarPorId(afiliadoId);
        assertThat(bajado).isPresent();
        assertThat(bajado.get().getEstado()).isEqualTo("BAJA");

        // When - Reactivar
        afiliadoUseCase.reactivar(afiliadoId);

        // Then
        Optional<AfiliadoDTO> reactivado = afiliadoUseCase.consultarPorId(afiliadoId);
        assertThat(reactivado).isPresent();
        assertThat(reactivado.get().getEstado()).isEqualTo("ACTIVO");
    }
}
```

## Pruebas de Arquitectura con ArchUnit

### HexagonalArchitectureTest.java

```java
package com.mutualidad.afiliado.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.mutualidad.afiliado")
@DisplayName("Hexagonal Architecture Tests")
class HexagonalArchitectureTest {

    // Regla 1: El dominio no debe depender de infraestructura
    @ArchTest
    static final ArchRule dominio_no_depende_de_infraestructura =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    // Regla 2: El dominio no debe depender de Spring
    @ArchTest
    static final ArchRule dominio_no_depende_de_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

    // Regla 3: El dominio no debe depender de JPA
    @ArchTest
    static final ArchRule dominio_no_depende_de_jpa =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("javax.persistence..");

    // Regla 4: Los adaptadores deben implementar puertos
    @ArchTest
    static final ArchRule adaptadores_output_implementan_puertos =
        classes()
            .that().resideInAPackage("..adapter.output..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implement(
                resideInAPackage("..port.output..")
            );

    // Regla 5: Los controladores solo deben usar puertos de entrada
    @ArchTest
    static final ArchRule controladores_usan_puertos_entrada =
        classes()
            .that().resideInAPackage("..adapter.input.rest..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..application.port.input..",
                "..application.dto..",
                "java..",
                "javax..",
                "org.springframework..",
                "lombok.."
            );

    // Regla 6: Arquitectura en capas
    @ArchTest
    static final ArchRule arquitectura_capas =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    // Regla 7: Las entidades de dominio deben ser inmutables o tener setters protegidos
    @ArchTest
    static final ArchRule entidades_sin_setters_publicos =
        noMethods()
            .that().areDeclaredInClassesThat()
            .resideInAPackage("..domain.model..")
            .and().haveNameStartingWith("set")
            .should().bePublic();
}
```

### CQRSArchitectureTest.java

```java
package com.mutualidad.beneficio.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "com.mutualidad.beneficio")
class CQRSArchitectureTest {

    // Regla 1: Commands deben ser inmutables (usar @Value)
    @ArchTest
    static final ArchRule commands_deben_ser_inmutables =
        classes()
            .that().resideInAPackage("..command.api..")
            .and().haveSimpleNameEndingWith("Command")
            .should().beAnnotatedWith(lombok.Value.class);

    // Regla 2: Query handlers no deben modificar estado
    @ArchTest
    static final ArchRule query_handlers_solo_lectura =
        noClasses()
            .that().resideInAPackage("..query.handler..")
            .should().dependOnClassesThat()
            .resideInAPackage("..command.repository..");

    // Regla 3: Command handlers no deben usar read repositories
    @ArchTest
    static final ArchRule command_handlers_no_usan_read_repo =
        noClasses()
            .that().resideInAPackage("..command.handler..")
            .should().dependOnClassesThat()
            .haveSimpleNameEndingWith("ReadRepository");

    // Regla 4: Eventos deben ser inmutables
    @ArchTest
    static final ArchRule eventos_inmutables =
        classes()
            .that().resideInAPackage("..event..")
            .and().haveSimpleNameEndingWith("Event")
            .should().beAnnotatedWith(lombok.Value.class);
}
```

## Configuración de Perfiles Spring

### application.yml (Base)

```yaml
spring:
  application:
    name: afiliado-service
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

# Configuración común
server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### application-dev.yml

```yaml
spring:
  config:
    activate:
      on-profile: dev
      
  datasource:
    url: jdbc:h2:mem:afiliadosdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  h2:
    console:
      enabled: true
      path: /h2-console
      
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# Servicios externos mock
validacion:
  service:
    url: http://localhost:8081
    mock-enabled: true

notification:
  email:
    enabled: false

logging:
  level:
    com.mutualidad: DEBUG
    org.springframework.web: DEBUG
```

### application-test.yml

```yaml
spring:
  config:
    activate:
      on-profile: test
      
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    
  jpa:
    hibernate:
      ddl-auto: create-drop

# Mock todos los servicios externos
validacion:
  service:
    url: http://localhost:9999
    mock-enabled: true

notification:
  email:
    enabled: false
    
kafka:
  enabled: false

logging:
  level:
    root: WARN
    com.mutualidad: INFO
```

### application-prod.yml

```yaml
spring:
  config:
    activate:
      on-profile: prod
      
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:afiliados}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

validacion:
  service:
    url: ${VALIDACION_SERVICE_URL}
    mock-enabled: false
    timeout: 5000
    
notification:
  email:
    enabled: true
    from: ${EMAIL_FROM:noreply@mutualidad.com}

kafka:
  bootstrap-servers: ${KAFKA_SERVERS}
  enabled: true

logging:
  level:
    root: WARN
    com.mutualidad: INFO
  file:
    name: /var/log/afiliado-service/application.log
```

## Test Configuration

### TestConfiguration.java

```java
package com.mutualidad.afiliado.config;

import com.mutualidad.afiliado.application.port.output.EventPublisherPort;
import com.mutualidad.afiliado.application.port.output.NotificacionPort;
import com.mutualidad.afiliado.application.port.output.ValidacionExternaPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfiguration {

    @Bean
    @Primary
    public ValidacionExternaPort mockValidacionPort() {
        return new ValidacionExternaPort() {
            @Override
            public boolean validarDocumento(Documento documento) {
                return true; // Siempre válido en tests
            }

            @Override
            public boolean verificarEstadoLaboral(String codigoEmpresa, String doc) {
                return true;
            }
        };
    }

    @Bean
    @Primary
    public EventPublisherPort mockEventPublisher() {
        return event -> {}; // No-op
    }

    @Bean
    @Primary
    public NotificacionPort mockNotificacionPort() {
        return new NotificacionPort() {
            @Override
            public void enviarBienvenida(String email, String nombre) {}

            @Override
            public void notificarBaja(String email, String nombre, String motivo) {}

            @Override
            public void notificarReactivacion(String email, String nombre) {}
        };
    }
}
```

## Resumen de Tipos de Pruebas

| Tipo | Ubicación | Perfil | Propósito |
|------|-----------|--------|-----------|
| **Unitarias** | `src/test/java` | N/A | Lógica de dominio aislada |
| **Integración** | `src/test/java` | `test` | Flujos completos con BD |
| **Arquitectura** | `src/test/java` | N/A | Validar reglas hexagonales |
| **Contrato** | `src/test/java` | `test` | APIs entre servicios |
| **E2E** | `src/e2e/java` | `e2e` | Flujos de negocio completos |
