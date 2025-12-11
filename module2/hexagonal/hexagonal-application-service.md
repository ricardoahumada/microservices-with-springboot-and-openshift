# Servicio de Aplicación - Arquitectura Hexagonal

## AfiliadoApplicationService.java

```java
package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;
import com.mutualidad.afiliado.application.port.input.AfiliadoUseCase;
import com.mutualidad.afiliado.application.port.output.AfiliadoRepository;
import com.mutualidad.afiliado.application.port.output.EventPublisherPort;
import com.mutualidad.afiliado.application.port.output.NotificacionPort;
import com.mutualidad.afiliado.application.port.output.ValidacionExternaPort;
import com.mutualidad.afiliado.domain.event.AfiliadoActivadoEvent;
import com.mutualidad.afiliado.domain.event.AfiliadoDadoDeBajaEvent;
import com.mutualidad.afiliado.domain.event.AfiliadoReactivadoEvent;
import com.mutualidad.afiliado.domain.event.AfiliadoRegistradoEvent;
import com.mutualidad.afiliado.domain.exception.AfiliadoNoEncontradoException;
import com.mutualidad.afiliado.domain.exception.AfiliadoYaExisteException;
import com.mutualidad.afiliado.domain.exception.DocumentoInvalidoException;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import com.mutualidad.afiliado.domain.model.TipoDocumento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio de aplicación que orquesta los casos de uso.
 * No contiene lógica de negocio, solo coordina la ejecución.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AfiliadoApplicationService implements AfiliadoUseCase {

    private final AfiliadoRepository afiliadoRepository;
    private final ValidacionExternaPort validacionExterna;
    private final EventPublisherPort eventPublisher;
    private final NotificacionPort notificacionPort;
    private final AfiliadoMapper mapper;

    @Override
    public AfiliadoDTO registrarAfiliado(RegistrarAfiliadoCommand command) {
        log.info("Registrando afiliado con documento: {}", command.getNumeroDocumento());

        // 1. Crear Value Object de documento
        Documento documento = new Documento(
            TipoDocumento.valueOf(command.getTipoDocumento()),
            command.getNumeroDocumento()
        );

        // 2. Verificar que no exista
        if (afiliadoRepository.existsByDocumento(documento)) {
            throw new AfiliadoYaExisteException(comando.getNumeroDocumento());
        }

        // 3. Validar documento externamente
        if (!validacionExterna.validarDocumento(documento)) {
            throw new DocumentoInvalidoException(command.getNumeroDocumento());
        }

        // 4. Verificar estado laboral
        boolean estadoLaboralActivo = validacionExterna.verificarEstadoLaboral(
            command.getCodigoEmpresa(),
            command.getNumeroDocumento()
        );

        // 5. Crear entidad de dominio (incluye validaciones de negocio)
        Afiliado afiliado = Afiliado.crear(
            documento,
            command.getNombre(),
            command.getPrimerApellido(),
            command.getSegundoApellido(),
            command.getFechaNacimiento(),
            command.getEmail(),
            command.getTelefono(),
            command.getDireccion(),
            command.getCodigoPostal(),
            command.getProvincia(),
            command.getCodigoEmpresa()
        );

        // 6. Activar si el estado laboral está activo
        if (estadoLaboralActivo) {
            afiliado.activar();
        }

        // 7. Persistir
        Afiliado afiliadoGuardado = afiliadoRepository.save(afiliado);

        // 8. Publicar evento
        eventPublisher.publish(new AfiliadoRegistradoEvent(
            afiliadoGuardado.getId(),
            documento.getTipo().name(),
            documento.getNumero(),
            afiliadoGuardado.getNombreCompleto(),
            afiliadoGuardado.getCodigoEmpresa()
        ));

        if (estadoLaboralActivo) {
            eventPublisher.publish(new AfiliadoActivadoEvent(afiliadoGuardado.getId()));
            
            // 9. Enviar notificación de bienvenida
            notificacionPort.enviarBienvenida(
                afiliadoGuardado.getEmail(),
                afiliadoGuardado.getNombreCompleto()
            );
        }

        log.info("Afiliado registrado con ID: {}", afiliadoGuardado.getId());
        return mapper.toDTO(afiliadoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AfiliadoDTO> consultarPorDocumento(String tipoDocumento, String numeroDocumento) {
        log.debug("Consultando afiliado por documento: {}/{}", tipoDocumento, numeroDocumento);

        Documento documento = new Documento(
            TipoDocumento.valueOf(tipoDocumento),
            numeroDocumento
        );

        return afiliadoRepository.findByDocumento(documento)
            .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AfiliadoDTO> consultarPorId(String afiliadoId) {
        log.debug("Consultando afiliado por ID: {}", afiliadoId);
        return afiliadoRepository.findById(afiliadoId)
            .map(mapper::toDTO);
    }

    @Override
    public void darDeBaja(String afiliadoId, String motivo) {
        log.info("Dando de baja afiliado: {} por motivo: {}", afiliadoId, motivo);

        // 1. Buscar afiliado
        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new AfiliadoNoEncontradoException(afiliadoId));

        // 2. Ejecutar lógica de dominio (incluye validaciones)
        afiliado.darDeBaja(motivo);

        // 3. Persistir
        afiliadoRepository.save(afiliado);

        // 4. Publicar evento
        eventPublisher.publish(new AfiliadoDadoDeBajaEvent(afiliadoId, motivo));

        // 5. Notificar
        notificacionPort.notificarBaja(
            afiliado.getEmail(),
            afiliado.getNombreCompleto(),
            motivo
        );

        log.info("Afiliado {} dado de baja exitosamente", afiliadoId);
    }

    @Override
    public void reactivar(String afiliadoId) {
        log.info("Reactivando afiliado: {}", afiliadoId);

        // 1. Buscar afiliado
        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new AfiliadoNoEncontradoException(afiliadoId));

        // 2. Verificar estado laboral actual
        boolean estadoLaboralActivo = validacionExterna.verificarEstadoLaboral(
            afiliado.getCodigoEmpresa(),
            afiliado.getDocumento().getNumero()
        );

        if (!estadoLaboralActivo) {
            throw new IllegalStateException(
                "No se puede reactivar: el estado laboral no está activo"
            );
        }

        // 3. Ejecutar lógica de dominio
        afiliado.reactivar();

        // 4. Persistir
        afiliadoRepository.save(afiliado);

        // 5. Publicar evento
        eventPublisher.publish(new AfiliadoReactivadoEvent(afiliadoId));

        // 6. Notificar
        notificacionPort.notificarReactivacion(
            afiliado.getEmail(),
            afiliado.getNombreCompleto()
        );

        log.info("Afiliado {} reactivado exitosamente", afiliadoId);
    }

    @Override
    public AfiliadoDTO actualizarContacto(String afiliadoId, String email, String telefono) {
        log.info("Actualizando contacto del afiliado: {}", afiliadoId);

        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new AfiliadoNoEncontradoException(afiliadoId));

        afiliado.actualizarContacto(email, telefono);

        Afiliado actualizado = afiliadoRepository.save(afiliado);

        log.info("Contacto actualizado para afiliado: {}", afiliadoId);
        return mapper.toDTO(actualizado);
    }
}
```

## AfiliadoMapper.java

```java
package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.domain.model.Afiliado;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio y DTOs de aplicación.
 */
@Component
public class AfiliadoMapper {

    public AfiliadoDTO toDTO(Afiliado afiliado) {
        return AfiliadoDTO.builder()
            .id(afiliado.getId())
            .tipoDocumento(afiliado.getDocumento().getTipo().name())
            .numeroDocumento(afiliado.getDocumento().getNumero())
            .nombreCompleto(afiliado.getNombreCompleto())
            .fechaNacimiento(afiliado.getFechaNacimiento())
            .email(afiliado.getEmail())
            .telefono(afiliado.getTelefono())
            .direccion(afiliado.getDireccion())
            .estado(afiliado.getEstado().name())
            .fechaAlta(afiliado.getFechaAlta())
            .fechaBaja(afiliado.getFechaBaja())
            .motivoBaja(afiliado.getMotivoBaja())
            .codigoEmpresa(afiliado.getCodigoEmpresa())
            .build();
    }
}
```

## Configuración de Beans

```java
package com.mutualidad.afiliado.infrastructure.config;

import com.mutualidad.afiliado.application.port.output.EventPublisherPort;
import com.mutualidad.afiliado.application.port.output.NotificacionPort;
import com.mutualidad.afiliado.infrastructure.adapter.output.event.KafkaEventPublisher;
import com.mutualidad.afiliado.infrastructure.adapter.output.notification.EmailNotificationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class BeanConfiguration {

    @Bean
    @Profile("!test")
    public EventPublisherPort eventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher(kafkaTemplate);
    }

    @Bean
    @Profile("!test")
    public NotificacionPort notificacionPort(JavaMailSender mailSender) {
        return new EmailNotificationAdapter(mailSender);
    }

    @Bean
    @Profile("test")
    public EventPublisherPort testEventPublisher() {
        return new InMemoryEventPublisher();
    }

    @Bean
    @Profile("test")
    public NotificacionPort testNotificacionPort() {
        return new NoOpNotificationAdapter();
    }
}
```

## Flujo de Orquestación

```
┌─────────────────────────────────────────────────────────────────┐
│                  FLUJO: REGISTRAR AFILIADO                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. VALIDAR ENTRADA                                             │
│     - Crear Value Object Documento                              │
│     - Verificar formato                                         │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. VERIFICAR UNICIDAD                                          │
│     - Consultar repositorio                                     │
│     - Lanzar excepción si existe                                │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. VALIDACIÓN EXTERNA                                          │
│     - Llamar ValidacionExternaPort                              │
│     - Verificar documento                                       │
│     - Verificar estado laboral                                  │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. CREAR ENTIDAD DE DOMINIO                                    │
│     - Afiliado.crear() con validaciones                         │
│     - Activar si corresponde                                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. PERSISTIR                                                   │
│     - afiliadoRepository.save()                                 │
│     - Dentro de transacción                                     │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. PUBLICAR EVENTOS                                            │
│     - AfiliadoRegistradoEvent                                   │
│     - AfiliadoActivadoEvent (si aplica)                         │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  7. NOTIFICAR                                                   │
│     - Enviar email de bienvenida                                │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  8. RETORNAR DTO                                                │
│     - Mapear entidad a DTO                                      │
│     - Devolver al adaptador                                     │
└─────────────────────────────────────────────────────────────────┘
```

## Principios del Servicio de Aplicación

| Principio | Descripción |
|-----------|-------------|
| **Solo orquestación** | No contiene lógica de negocio |
| **Transaccional** | Marca límites de transacción |
| **Logging** | Registra operaciones importantes |
| **Inyección de puertos** | Depende solo de interfaces |
| **Manejo de excepciones** | Traduce a excepciones de aplicación |
