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

        Documento documento = new Documento(
            TipoDocumento.valueOf(command.getTipoDocumento()),
            command.getNumeroDocumento()
        );

        if (afiliadoRepository.existsByDocumento(documento)) {
            throw new AfiliadoYaExisteException(command.getNumeroDocumento());
        }

        if (!validacionExterna.validarDocumento(documento)) {
            throw new DocumentoInvalidoException(command.getNumeroDocumento());
        }

        boolean estadoLaboralActivo = validacionExterna.verificarEstadoLaboral(
            command.getCodigoEmpresa(),
            command.getNumeroDocumento()
        );

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

        if (estadoLaboralActivo) {
            afiliado.activar();
        }

        Afiliado afiliadoGuardado = afiliadoRepository.save(afiliado);

        eventPublisher.publish(new AfiliadoRegistradoEvent(
            afiliadoGuardado.getId(),
            documento.getTipo().name(),
            documento.getNumero(),
            afiliadoGuardado.getNombreCompleto(),
            afiliadoGuardado.getCodigoEmpresa()
        ));

        if (estadoLaboralActivo) {
            eventPublisher.publish(new AfiliadoActivadoEvent(afiliadoGuardado.getId()));
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

        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new AfiliadoNoEncontradoException(afiliadoId));

        afiliado.darDeBaja(motivo);
        afiliadoRepository.save(afiliado);

        eventPublisher.publish(new AfiliadoDadoDeBajaEvent(afiliadoId, motivo));
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

        Afiliado afiliado = afiliadoRepository.findById(afiliadoId)
            .orElseThrow(() -> new AfiliadoNoEncontradoException(afiliadoId));

        boolean estadoLaboralActivo = validacionExterna.verificarEstadoLaboral(
            afiliado.getCodigoEmpresa(),
            afiliado.getDocumento().getNumero()
        );

        if (!estadoLaboralActivo) {
            throw new IllegalStateException(
                "No se puede reactivar: el estado laboral no esta activo"
            );
        }

        afiliado.reactivar();
        afiliadoRepository.save(afiliado);

        eventPublisher.publish(new AfiliadoReactivadoEvent(afiliadoId));
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
