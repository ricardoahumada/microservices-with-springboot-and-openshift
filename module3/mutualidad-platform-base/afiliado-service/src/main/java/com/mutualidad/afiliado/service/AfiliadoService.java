package com.mutualidad.afiliado.service;

import com.mutualidad.afiliado.dto.AfiliadoResponse;
import com.mutualidad.afiliado.dto.AltaAfiliadoRequest;
import com.mutualidad.afiliado.entity.Afiliado;
import com.mutualidad.afiliado.exception.BusinessException;
import com.mutualidad.afiliado.repository.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    private final AfiliadoRepository afiliadoRepository;

    /**
     * Alta de afiliado basica.
     * En la version con resiliencia, se aniadiran:
     * - Validacion contra servicio externo
     * - Asignacion de beneficios
     * - Notificaciones
     */
    @Transactional
    public AfiliadoResponse altaAfiliado(AltaAfiliadoRequest request) {
        log.info("Iniciando alta de afiliado DNI: {}", request.getDni());

        // 1. Verificar si ya existe
        if (afiliadoRepository.existsByDni(request.getDni())) {
            throw new BusinessException("Ya existe un afiliado con DNI: " + request.getDni());
        }

        // 2. Crear afiliado (estado PENDIENTE por defecto)
        Afiliado afiliado = Afiliado.builder()
                .dni(request.getDni())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .fechaNacimiento(request.getFechaNacimiento())
                .empresaId(request.getEmpresaId())
                .estado("PENDIENTE")
                .build();

        afiliado = afiliadoRepository.save(afiliado);
        log.info("Afiliado creado con ID: {}", afiliado.getId());

        return buildResponse(afiliado, "Alta completada exitosamente");
    }

    public AfiliadoResponse obtenerPorId(Long id) {
        Afiliado afiliado = afiliadoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Afiliado no encontrado: " + id));
        
        return buildResponse(afiliado, null);
    }

    public AfiliadoResponse obtenerPorDni(String dni) {
        Afiliado afiliado = afiliadoRepository.findByDni(dni)
                .orElseThrow(() -> new BusinessException("Afiliado no encontrado con DNI: " + dni));
        
        return buildResponse(afiliado, null);
    }

    @Transactional
    public AfiliadoResponse activarAfiliado(Long id) {
        Afiliado afiliado = afiliadoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Afiliado no encontrado: " + id));
        
        afiliado.setEstado("ACTIVO");
        afiliado = afiliadoRepository.save(afiliado);
        
        return buildResponse(afiliado, "Afiliado activado exitosamente");
    }

    private AfiliadoResponse buildResponse(Afiliado afiliado, String mensaje) {
        return AfiliadoResponse.builder()
                .id(afiliado.getId())
                .dni(afiliado.getDni())
                .nombre(afiliado.getNombre())
                .apellido(afiliado.getApellido())
                .email(afiliado.getEmail())
                .telefono(afiliado.getTelefono())
                .fechaNacimiento(afiliado.getFechaNacimiento())
                .estado(afiliado.getEstado())
                .empresaId(afiliado.getEmpresaId())
                .fechaCreacion(afiliado.getFechaCreacion())
                .mensaje(mensaje)
                .build();
    }
}
