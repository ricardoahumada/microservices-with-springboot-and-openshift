# Handlers CQRS - Beneficio Service

## Command Handlers

Los Command Handlers procesan los commands, aplican la lógica de negocio y persisten los cambios.

### AsignarBeneficioHandler.java

```java
package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.AsignarBeneficioCommand;
import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.BeneficioAggregate;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioAsignadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsignarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final EventPublisher eventPublisher;
    private final AfiliadoValidationService afiliadoValidation;

    @Transactional
    public CommandResult handle(@Valid AsignarBeneficioCommand cmd) {
        log.info("Procesando asignación de beneficio: tipo={}, afiliado={}",
            cmd.getTipoBeneficio(), cmd.getAfiliadoId());

        // 1. Validar que el afiliado existe y está activo
        if (!afiliadoValidation.estaActivo(cmd.getAfiliadoId())) {
            log.warn("Afiliado no activo: {}", cmd.getAfiliadoId());
            return CommandResult.failure("El afiliado no está activo");
        }

        // 2. Validar límites de beneficios por tipo
        List<Beneficio> beneficiosActivos = repository.findActivosByAfiliadoAndTipo(
            cmd.getAfiliadoId(),
            cmd.getTipoBeneficio()
        );

        if (!beneficiosActivos.isEmpty()) {
            log.warn("Ya existe beneficio activo del mismo tipo para afiliado: {}", 
                cmd.getAfiliadoId());
            return CommandResult.failure(
                "Ya existe un beneficio activo de tipo " + cmd.getTipoBeneficio()
            );
        }

        // 3. Validar monto si el tipo lo requiere
        if (cmd.getTipoBeneficio().requiereMonto() && cmd.getMonto() == null) {
            return CommandResult.failure("Este tipo de beneficio requiere especificar monto");
        }

        // 4. Crear el aggregate y aplicar la lógica de dominio
        BeneficioAggregate aggregate = BeneficioAggregate.crear(
            cmd.getAfiliadoId(),
            cmd.getTipoBeneficio(),
            cmd.getFechaInicio(),
            cmd.getFechaFin(),
            cmd.getMonto(),
            cmd.getDescripcion(),
            cmd.getSolicitadoPor()
        );

        // 5. Persistir
        Beneficio beneficio = aggregate.getBeneficio();
        repository.save(beneficio);

        // 6. Publicar evento
        eventPublisher.publish(new BeneficioAsignadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            beneficio.getTipoBeneficio().name(),
            beneficio.getFechaInicio(),
            beneficio.getMonto()
        ));

        log.info("Beneficio asignado exitosamente: id={}", beneficio.getId());
        return CommandResult.success(beneficio.getId());
    }
}
```

### RevocarBeneficioHandler.java

```java
package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.api.RevocarBeneficioCommand;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.EstadoBeneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.event.BeneficioRevocadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevocarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CommandResult handle(@Valid RevocarBeneficioCommand cmd) {
        log.info("Procesando revocación de beneficio: id={}", cmd.getBeneficioId());

        // 1. Buscar beneficio
        Beneficio beneficio = repository.findById(cmd.getBeneficioId())
            .orElse(null);

        if (beneficio == null) {
            return CommandResult.failure("Beneficio no encontrado");
        }

        // 2. Validar que se puede revocar
        if (beneficio.getEstado() == EstadoBeneficio.REVOCADO) {
            return CommandResult.failure("El beneficio ya está revocado");
        }

        if (beneficio.getEstado() == EstadoBeneficio.EXPIRADO) {
            return CommandResult.failure("No se puede revocar un beneficio expirado");
        }

        // 3. Aplicar revocación
        LocalDate fechaEfectiva = cmd.getFechaEfectiva() != null 
            ? cmd.getFechaEfectiva() 
            : LocalDate.now();

        beneficio.revocar(cmd.getMotivo(), fechaEfectiva, cmd.getRevocadoPor());

        // 4. Persistir
        repository.save(beneficio);

        // 5. Publicar evento
        eventPublisher.publish(new BeneficioRevocadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            cmd.getMotivo(),
            fechaEfectiva
        ));

        log.info("Beneficio revocado exitosamente: id={}", cmd.getBeneficioId());
        return CommandResult.success(cmd.getBeneficioId());
    }
}
```

### ModificarBeneficioHandler.java

```java
package com.mutualidad.beneficio.command.handler;

import com.mutualidad.beneficio.command.api.CommandResult;
import com.mutualidad.beneficio.command.api.ModificarBeneficioCommand;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.CambioHistorico;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.command.repository.CambioHistoricoRepository;
import com.mutualidad.beneficio.event.BeneficioModificadoEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModificarBeneficioHandler {

    private final BeneficioWriteRepository repository;
    private final CambioHistoricoRepository historicoRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public CommandResult handle(@Valid ModificarBeneficioCommand cmd) {
        log.info("Procesando modificación de beneficio: id={}", cmd.getBeneficioId());

        // 1. Buscar beneficio
        Beneficio beneficio = repository.findById(cmd.getBeneficioId())
            .orElse(null);

        if (beneficio == null) {
            return CommandResult.failure("Beneficio no encontrado");
        }

        // 2. Validar que se puede modificar
        if (!beneficio.esModificable()) {
            return CommandResult.failure(
                "El beneficio en estado " + beneficio.getEstado() + " no es modificable"
            );
        }

        // 3. Guardar estado anterior para historial
        String estadoAnterior = beneficio.toJson();

        // 4. Aplicar modificaciones
        if (cmd.getNuevoMonto() != null) {
            beneficio.actualizarMonto(cmd.getNuevoMonto());
        }
        if (cmd.getNuevaFechaFin() != null) {
            beneficio.actualizarFechaFin(cmd.getNuevaFechaFin());
        }
        if (cmd.getNuevaDescripcion() != null) {
            beneficio.actualizarDescripcion(cmd.getNuevaDescripcion());
        }

        // 5. Registrar en historial (para auditoría)
        CambioHistorico cambio = CambioHistorico.builder()
            .beneficioId(cmd.getBeneficioId())
            .estadoAnterior(estadoAnterior)
            .estadoNuevo(beneficio.toJson())
            .motivo(cmd.getMotivo())
            .modificadoPor(cmd.getModificadoPor())
            .fechaModificacion(LocalDateTime.now())
            .build();
        historicoRepository.save(cambio);

        // 6. Persistir beneficio
        repository.save(beneficio);

        // 7. Publicar evento
        eventPublisher.publish(new BeneficioModificadoEvent(
            beneficio.getId(),
            beneficio.getAfiliadoId(),
            cmd.getMotivo(),
            cmd.getModificadoPor()
        ));

        log.info("Beneficio modificado exitosamente: id={}", cmd.getBeneficioId());
        return CommandResult.success(cmd.getBeneficioId());
    }
}
```

## Query Handlers

### BuscarBeneficiosHandler.java

```java
package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.api.BuscarBeneficiosQuery;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuscarBeneficiosHandler {

    private final BeneficioReadRepository readRepository;

    public Page<BeneficioReadModel> handle(BuscarBeneficiosQuery query) {
        log.debug("Buscando beneficios: afiliado={}, estado={}", 
            query.getAfiliadoId(), query.getEstado());

        PageRequest pageRequest = PageRequest.of(
            query.getPage(),
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "fechaInicio")
        );

        if (query.getEstado() != null) {
            return readRepository.findByAfiliadoIdAndEstado(
                query.getAfiliadoId(),
                query.getEstado().name(),
                pageRequest
            );
        }

        return readRepository.findByAfiliadoId(query.getAfiliadoId(), pageRequest);
    }
}
```

### ObtenerHistorialHandler.java

```java
package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.api.ObtenerHistorialQuery;
import com.mutualidad.beneficio.query.model.CambioHistoricoReadModel;
import com.mutualidad.beneficio.query.repository.CambioHistoricoReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ObtenerHistorialHandler {

    private final CambioHistoricoReadRepository readRepository;

    public Page<CambioHistoricoReadModel> handle(ObtenerHistorialQuery query) {
        log.debug("Obteniendo historial de beneficio: {}", query.getBeneficioId());

        PageRequest pageRequest = PageRequest.of(
            query.getPage(),
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "fechaModificacion")
        );

        if (query.getFechaDesde() != null && query.getFechaHasta() != null) {
            return readRepository.findByBeneficioIdAndFechaBetween(
                query.getBeneficioId(),
                query.getFechaDesde(),
                query.getFechaHasta(),
                pageRequest
            );
        }

        return readRepository.findByBeneficioId(query.getBeneficioId(), pageRequest);
    }
}
```

## Command Bus (Opcional)

### CommandBus.java

```java
package com.mutualidad.beneficio.command;

import com.mutualidad.beneficio.command.api.*;
import com.mutualidad.beneficio.command.handler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bus de comandos que enruta cada command a su handler correspondiente.
 * Proporciona un punto de entrada único para todos los commands.
 */
@Component
@RequiredArgsConstructor
public class CommandBus {

    private final AsignarBeneficioHandler asignarHandler;
    private final RevocarBeneficioHandler revocarHandler;
    private final ModificarBeneficioHandler modificarHandler;
    private final SuspenderBeneficioHandler suspenderHandler;
    private final ReactivarBeneficioHandler reactivarHandler;

    public CommandResult dispatch(Object command) {
        if (command instanceof AsignarBeneficioCommand cmd) {
            return asignarHandler.handle(cmd);
        }
        if (command instanceof RevocarBeneficioCommand cmd) {
            return revocarHandler.handle(cmd);
        }
        if (command instanceof ModificarBeneficioCommand cmd) {
            return modificarHandler.handle(cmd);
        }
        if (command instanceof SuspenderBeneficioCommand cmd) {
            return suspenderHandler.handle(cmd);
        }
        if (command instanceof ReactivarBeneficioCommand cmd) {
            return reactivarHandler.handle(cmd);
        }

        throw new IllegalArgumentException(
            "No hay handler registrado para: " + command.getClass().getSimpleName()
        );
    }
}
```

## Diagrama de Flujo de Handlers

```
┌─────────────────────────────────────────────────────────────────┐
│                    COMMAND HANDLER FLOW                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. RECIBIR COMMAND                                             │
│     - Validar con Bean Validation                               │
│     - Log de entrada                                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. VALIDAR PRE-CONDICIONES                                     │
│     - Verificar existencia de entidades                         │
│     - Validar reglas de negocio                                 │
│     - Retornar CommandResult.failure() si falla                 │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. APLICAR LÓGICA DE DOMINIO                                   │
│     - Cargar/crear Aggregate                                    │
│     - Ejecutar métodos de dominio                               │
│     - Validaciones adicionales en el dominio                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. PERSISTIR CAMBIOS                                           │
│     - Guardar en Write Repository                               │
│     - Guardar historial si aplica                               │
│     - Transacción atómica                                       │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. PUBLICAR EVENTOS                                            │
│     - Crear evento de dominio                                   │
│     - Publicar a través de EventPublisher                       │
│     - Los eventos actualizan el Read Model                      │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. RETORNAR RESULTADO                                          │
│     - CommandResult.success(id)                                 │
│     - Log de salida                                             │
└─────────────────────────────────────────────────────────────────┘
```
