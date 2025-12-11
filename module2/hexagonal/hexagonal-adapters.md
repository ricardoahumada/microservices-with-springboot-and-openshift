# Adaptadores - Arquitectura Hexagonal

## Adaptadores Primarios (Driving)

### AfiliadoController.java - Adaptador REST

```java
package com.mutualidad.afiliado.infrastructure.adapter.input.rest;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;
import com.mutualidad.afiliado.application.port.input.AfiliadoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Adaptador primario REST que expone los casos de uso de afiliados.
 * Traduce peticiones HTTP a llamadas al puerto de entrada.
 */
@RestController
@RequestMapping("/api/v1/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoUseCase afiliadoUseCase;

    @PostMapping
    public ResponseEntity<AfiliadoDTO> registrar(
            @Valid @RequestBody RegistrarAfiliadoRequest request) {
        
        RegistrarAfiliadoCommand command = mapToCommand(request);
        AfiliadoDTO resultado = afiliadoUseCase.registrarAfiliado(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(resultado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AfiliadoDTO> consultarPorId(@PathVariable String id) {
        return afiliadoUseCase.consultarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/documento/{tipo}/{numero}")
    public ResponseEntity<AfiliadoDTO> consultarPorDocumento(
            @PathVariable String tipo,
            @PathVariable String numero) {
        return afiliadoUseCase.consultarPorDocumento(tipo, numero)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/baja")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable String id,
            @RequestBody DarDeBajaRequest request) {
        afiliadoUseCase.darDeBaja(id, request.getMotivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable String id) {
        afiliadoUseCase.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    private RegistrarAfiliadoCommand mapToCommand(RegistrarAfiliadoRequest request) {
        return RegistrarAfiliadoCommand.builder()
            .tipoDocumento(request.getTipoDocumento())
            .numeroDocumento(request.getNumeroDocumento())
            .nombre(request.getNombre())
            .primerApellido(request.getPrimerApellido())
            .segundoApellido(request.getSegundoApellido())
            .fechaNacimiento(request.getFechaNacimiento())
            .email(request.getEmail())
            .telefono(request.getTelefono())
            .direccion(request.getDireccion())
            .codigoPostal(request.getCodigoPostal())
            .provincia(request.getProvincia())
            .codigoEmpresa(request.getCodigoEmpresa())
            .build();
    }
}
```

### Request DTOs

```java
package com.mutualidad.afiliado.infrastructure.adapter.input.rest;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistrarAfiliadoRequest {
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    private String email;
    private String telefono;
    private String direccion;
    private String codigoPostal;
    private String provincia;
    private String codigoEmpresa;
}

@Data
public class DarDeBajaRequest {
    private String motivo;
}
```

## Adaptadores Secundarios (Driven)

### AfiliadoJpaAdapter.java - Adaptador de Persistencia

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.application.port.output.AfiliadoRepository;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador secundario que implementa el puerto de persistencia.
 * Traduce entre entidades de dominio y entidades JPA.
 */
@Component
@RequiredArgsConstructor
public class AfiliadoJpaAdapter implements AfiliadoRepository {

    private final AfiliadoJpaRepository jpaRepository;
    private final AfiliadoMapper mapper;

    @Override
    public Afiliado save(Afiliado afiliado) {
        AfiliadoEntity entity = mapper.toEntity(afiliado);
        AfiliadoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Afiliado> findById(String id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Afiliado> findByDocumento(Documento documento) {
        return jpaRepository.findByTipoDocumentoAndNumeroDocumento(
                documento.getTipo().name(),
                documento.getNumero()
            )
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByDocumento(Documento documento) {
        return jpaRepository.existsByTipoDocumentoAndNumeroDocumento(
            documento.getTipo().name(),
            documento.getNumero()
        );
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
```

### AfiliadoEntity.java - Entidad JPA

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "afiliados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfiliadoEntity {

    @Id
    private String id;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false)
    private String numeroDocumento;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "primer_apellido", nullable = false)
    private String primerApellido;

    @Column(name = "segundo_apellido")
    private String segundoApellido;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    private String email;
    private String telefono;
    private String direccion;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private String provincia;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "motivo_baja")
    private String motivoBaja;

    @Column(name = "codigo_empresa", nullable = false)
    private String codigoEmpresa;

    // Índice compuesto para búsqueda por documento
    @Table(indexes = {
        @Index(name = "idx_documento", columnList = "tipo_documento, numero_documento", unique = true)
    })
    private static class Indexes {}
}
```

### ValidacionRestAdapter.java - Adaptador de Cliente Externo

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.external;

import com.mutualidad.afiliado.application.port.output.ValidacionExternaPort;
import com.mutualidad.afiliado.domain.model.Documento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adaptador que conecta con el servicio externo de validación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidacionRestAdapter implements ValidacionExternaPort {

    private final RestTemplate restTemplate;

    @Value("${validacion.service.url}")
    private String validacionServiceUrl;

    @Override
    public boolean validarDocumento(Documento documento) {
        try {
            String url = String.format("%s/api/v1/validar/documento/%s/%s",
                validacionServiceUrl,
                documento.getTipo(),
                documento.getNumero());

            ValidacionResponse response = restTemplate.getForObject(
                url, 
                ValidacionResponse.class
            );

            return response != null && response.isValido();
        } catch (Exception e) {
            log.error("Error validando documento: {}", e.getMessage());
            // En caso de error, asumimos que es válido (fail-open)
            // En producción, esto debería ser configurable
            return true;
        }
    }

    @Override
    public boolean verificarEstadoLaboral(String codigoEmpresa, String numeroDocumento) {
        try {
            String url = String.format("%s/api/v1/validar/laboral/%s/%s",
                validacionServiceUrl,
                codigoEmpresa,
                numeroDocumento);

            EstadoLaboralResponse response = restTemplate.getForObject(
                url, 
                EstadoLaboralResponse.class
            );

            return response != null && response.isActivo();
        } catch (Exception e) {
            log.error("Error verificando estado laboral: {}", e.getMessage());
            return true;
        }
    }
}

@Data
class ValidacionResponse {
    private boolean valido;
    private String mensaje;
}

@Data
class EstadoLaboralResponse {
    private boolean activo;
    private String estado;
    private String fechaAlta;
}
```

## Mapper entre Dominio y Entidad JPA

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import com.mutualidad.afiliado.domain.model.TipoDocumento;
import org.springframework.stereotype.Component;

@Component
public class AfiliadoMapper {

    public AfiliadoEntity toEntity(Afiliado afiliado) {
        return AfiliadoEntity.builder()
            .id(afiliado.getId())
            .tipoDocumento(afiliado.getDocumento().getTipo().name())
            .numeroDocumento(afiliado.getDocumento().getNumero())
            .nombre(afiliado.getNombre())
            .primerApellido(afiliado.getPrimerApellido())
            .segundoApellido(afiliado.getSegundoApellido())
            .fechaNacimiento(afiliado.getFechaNacimiento())
            .email(afiliado.getEmail())
            .telefono(afiliado.getTelefono())
            .direccion(afiliado.getDireccion())
            .codigoPostal(afiliado.getCodigoPostal())
            .provincia(afiliado.getProvincia())
            .estado(afiliado.getEstado().name())
            .fechaAlta(afiliado.getFechaAlta())
            .fechaBaja(afiliado.getFechaBaja())
            .motivoBaja(afiliado.getMotivoBaja())
            .codigoEmpresa(afiliado.getCodigoEmpresa())
            .build();
    }

    public Afiliado toDomain(AfiliadoEntity entity) {
        Documento documento = new Documento(
            TipoDocumento.valueOf(entity.getTipoDocumento()),
            entity.getNumeroDocumento()
        );

        return Afiliado.reconstitute(
            entity.getId(),
            documento,
            entity.getNombre(),
            entity.getPrimerApellido(),
            entity.getSegundoApellido(),
            entity.getFechaNacimiento(),
            entity.getEmail(),
            entity.getTelefono(),
            entity.getDireccion(),
            entity.getCodigoPostal(),
            entity.getProvincia(),
            EstadoAfiliado.valueOf(entity.getEstado()),
            entity.getFechaAlta(),
            entity.getFechaBaja(),
            entity.getMotivoBaja(),
            entity.getCodigoEmpresa()
        );
    }
}
```

## Notas de Diseño

1. **Separación clara**: El adaptador REST no conoce la persistencia
2. **Mapeo explícito**: Los mappers traducen entre capas sin acoplarlas
3. **Configuración externa**: URLs de servicios en application.yml
4. **Manejo de errores**: Los adaptadores manejan excepciones de infraestructura
5. **Logging**: Trazabilidad de operaciones externas
