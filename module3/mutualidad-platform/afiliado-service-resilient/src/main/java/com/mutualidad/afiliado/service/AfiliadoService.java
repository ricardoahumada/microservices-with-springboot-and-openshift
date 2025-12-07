package com.mutualidad.afiliado.service;

import com.mutualidad.afiliado.client.BeneficioServiceClient;
import com.mutualidad.afiliado.client.NotificacionServiceClient;
import com.mutualidad.afiliado.client.ValidacionServiceClient;
import com.mutualidad.afiliado.client.dto.NotificacionResponse;
import com.mutualidad.afiliado.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.client.dto.ValidacionResponse;
import com.mutualidad.afiliado.dto.AfiliadoResponse;
import com.mutualidad.afiliado.dto.AltaAfiliadoRequest;
import com.mutualidad.afiliado.dto.BeneficioDto;
import com.mutualidad.afiliado.entity.Afiliado;
import com.mutualidad.afiliado.exception.BusinessException;
import com.mutualidad.afiliado.filter.CorrelationIdFilter;
import com.mutualidad.afiliado.repository.AfiliadoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    private final AfiliadoRepository afiliadoRepository;
    private final ValidacionServiceClient validacionServiceClient;
    private final BeneficioServiceClient beneficioServiceClient;
    private final NotificacionServiceClient notificacionServiceClient;

    /**
     * Alta de afiliado con validacion, asignacion de beneficios y notificacion.
     * Implementa patrones de resiliencia: Circuit Breaker, Retry, Timeout.
     */
    @Transactional
    public AfiliadoResponse altaAfiliado(AltaAfiliadoRequest request) {
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        log.info("[{}] Iniciando alta de afiliado DNI: {}", correlationId, request.getDni());

        // 1. Verificar si ya existe
        if (afiliadoRepository.existsByDni(request.getDni())) {
            throw new BusinessException("Ya existe un afiliado con DNI: " + request.getDni());
        }

        // 2. Validar estado laboral (con resiliencia)
        ValidacionResponse validacion = validarEstadoLaboralConResiliencia(request.getDni(), request.getEmpresaId());
        log.info("[{}] Resultado validacion: {} - {}", correlationId, validacion.getEstado(), validacion.getMensaje());

        // 3. Crear afiliado
        Afiliado afiliado = crearAfiliado(request, validacion);
        log.info("[{}] Afiliado creado con ID: {}", correlationId, afiliado.getId());

        // 4. Asignar beneficios basicos (con resiliencia)
        List<BeneficioDto> beneficios = asignarBeneficiosConResiliencia(afiliado.getId());
        log.info("[{}] Beneficios asignados: {}", correlationId, beneficios.size());

        // 5. Enviar notificacion de bienvenida (con resiliencia, fire-and-forget)
        enviarNotificacionConResiliencia(afiliado.getId(), afiliado.getEmail());

        return buildResponse(afiliado, beneficios, "Alta completada exitosamente");
    }

    @CircuitBreaker(name = "validacionService", fallbackMethod = "validacionFallback")
    @Retry(name = "validacionService")
    @TimeLimiter(name = "validacionService")
    public CompletableFuture<ValidacionResponse> validarEstadoLaboralAsync(String dni, String empresaId) {
        return CompletableFuture.supplyAsync(() -> {
            ValidacionRequest request = ValidacionRequest.builder()
                    .dni(dni)
                    .empresaId(empresaId)
                    .build();
            return validacionServiceClient.validarEstadoLaboral(request, null);
        });
    }

    /**
     * Version sincrona con resiliencia mediante anotaciones.
     */
    @CircuitBreaker(name = "validacionService", fallbackMethod = "validacionFallbackSync")
    @Retry(name = "validacionService")
    public ValidacionResponse validarEstadoLaboralConResiliencia(String dni, String empresaId) {
        log.info("Llamando a servicio de validacion para DNI: {}", dni);
        ValidacionRequest request = ValidacionRequest.builder()
                .dni(dni)
                .empresaId(empresaId)
                .build();
        return validacionServiceClient.validarEstadoLaboral(request, null);
    }

    /**
     * Metodo para testing - permite simular errores.
     */
    @CircuitBreaker(name = "validacionService", fallbackMethod = "validacionFallbackSimulacion")
    @Retry(name = "validacionService")
    public ValidacionResponse validarConSimulacion(String dni, String empresaId, String simulateError) {
        log.info("Llamando a servicio de validacion (simulacion: {}) para DNI: {}", simulateError, dni);
        ValidacionRequest request = ValidacionRequest.builder()
                .dni(dni)
                .empresaId(empresaId)
                .build();
        return validacionServiceClient.validarEstadoLaboral(request, simulateError);
    }

    @CircuitBreaker(name = "beneficioService", fallbackMethod = "beneficiosFallback")
    @Retry(name = "beneficioService")
    public List<BeneficioDto> asignarBeneficiosConResiliencia(Long afiliadoId) {
        log.info("Llamando a servicio de beneficios para afiliado: {}", afiliadoId);
        return beneficioServiceClient.asignarBeneficiosBasicos(afiliadoId);
    }

    @CircuitBreaker(name = "notificacionService", fallbackMethod = "notificacionFallback")
    @Retry(name = "notificacionService")
    public NotificacionResponse enviarNotificacionConResiliencia(Long afiliadoId, String email) {
        log.info("Enviando notificacion de bienvenida al afiliado: {}", afiliadoId);
        return notificacionServiceClient.enviarBienvenida(afiliadoId, email != null ? email : "sin-email@mutualidad.com");
    }

    // ==================== FALLBACKS ====================

    public ValidacionResponse validacionFallbackSync(String dni, String empresaId, Throwable t) {
        log.warn("FALLBACK validacion (sync) - DNI: {}, Error: {}", dni, t.getMessage());
        return ValidacionResponse.builder()
                .valido(true)
                .estado("PENDIENTE_VERIFICACION")
                .mensaje("Validacion pendiente - " + t.getMessage())
                .dni(dni)
                .empresaId(empresaId)
                .build();
    }

    public ValidacionResponse validacionFallbackSimulacion(String dni, String empresaId, String simulateError, Throwable t) {
        log.warn("FALLBACK validacion (simulacion: {}) - DNI: {}, Error: {}", simulateError, dni, t.getMessage());
        return ValidacionResponse.builder()
                .valido(true)
                .estado("PENDIENTE_VERIFICACION")
                .mensaje("Fallback activo - Error: " + t.getMessage())
                .dni(dni)
                .empresaId(empresaId)
                .build();
    }

    public CompletableFuture<ValidacionResponse> validacionFallback(String dni, String empresaId, Throwable t) {
        log.warn("FALLBACK validacion - DNI: {}, Error: {}", dni, t.getMessage());
        return CompletableFuture.completedFuture(
                ValidacionResponse.builder()
                        .valido(true)
                        .estado("PENDIENTE_VERIFICACION")
                        .mensaje("Validacion pendiente - " + t.getMessage())
                        .dni(dni)
                        .empresaId(empresaId)
                        .build()
        );
    }

    public List<BeneficioDto> beneficiosFallback(Long afiliadoId, Throwable t) {
        log.warn("FALLBACK beneficios - AfiliadoId: {}, Error: {}", afiliadoId, t.getMessage());
        return Collections.emptyList();
    }

    public NotificacionResponse notificacionFallback(Long afiliadoId, String email, Throwable t) {
        log.warn("FALLBACK notificacion - AfiliadoId: {}, Error: {}", afiliadoId, t.getMessage());
        return NotificacionResponse.builder()
                .afiliadoId(afiliadoId)
                .estado("PENDIENTE")
                .mensaje("Notificacion pendiente - " + t.getMessage())
                .build();
    }

    // ==================== METODOS AUXILIARES ====================

    private Afiliado crearAfiliado(AltaAfiliadoRequest request, ValidacionResponse validacion) {
        String estado = validacion.isValido() ? "ACTIVO" : "PENDIENTE";
        
        Afiliado afiliado = Afiliado.builder()
                .dni(request.getDni())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .fechaNacimiento(request.getFechaNacimiento())
                .empresaId(request.getEmpresaId())
                .estado(estado)
                .build();

        return afiliadoRepository.save(afiliado);
    }

    private AfiliadoResponse buildResponse(Afiliado afiliado, List<BeneficioDto> beneficios, String mensaje) {
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
                .beneficios(beneficios)
                .mensaje(mensaje)
                .build();
    }

    public AfiliadoResponse obtenerPorId(Long id) {
        Afiliado afiliado = afiliadoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Afiliado no encontrado: " + id));
        
        List<BeneficioDto> beneficios = beneficioServiceClient.obtenerBeneficiosPorAfiliado(id);
        
        return buildResponse(afiliado, beneficios, null);
    }

    public AfiliadoResponse obtenerPorDni(String dni) {
        Afiliado afiliado = afiliadoRepository.findByDni(dni)
                .orElseThrow(() -> new BusinessException("Afiliado no encontrado con DNI: " + dni));
        
        List<BeneficioDto> beneficios = beneficioServiceClient.obtenerBeneficiosPorAfiliado(afiliado.getId());
        
        return buildResponse(afiliado, beneficios, null);
    }
}
