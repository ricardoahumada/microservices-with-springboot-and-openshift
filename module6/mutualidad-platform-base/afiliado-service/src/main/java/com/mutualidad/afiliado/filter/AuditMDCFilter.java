package com.mutualidad.afiliado.filter;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * TODO Ejercicio 1.3: Implementar filtro MDC para contexto de negocio
 * 
 * El MDC (Mapped Diagnostic Context) permite agregar informacion contextual
 * a cada linea de log, facilitando la trazabilidad de peticiones.
 * 
 * Pasos:
 * 1. Definir constantes para las claves MDC (requestId, userId, clientIp, etc.)
 * 2. En doFilter():
 *    - Generar o recuperar Request ID del header X-Request-ID
 *    - Agregar contexto al MDC con MDC.put(key, value)
 *    - Ejecutar chain.doFilter()
 *    - Limpiar MDC en finally con MDC.clear()
 * 3. Implementar getClientIp() para obtener IP real (considerando proxies)
 */
@Component
@Order(1)
public class AuditMDCFilter implements Filter {

    // TODO: Definir constantes para claves MDC
    // private static final String REQUEST_ID = "requestId";
    // private static final String USER_ID = "userId";
    // private static final String CLIENT_IP = "clientIp";
    // private static final String USER_AGENT = "userAgent";
    // private static final String REQUEST_URI = "requestUri";
    // private static final String REQUEST_METHOD = "requestMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // TODO: Implementar logica MDC
        // 
        // HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 
        // try {
        //     // Generar o recuperar Request ID
        //     String requestId = httpRequest.getHeader("X-Request-ID");
        //     if (requestId == null || requestId.isEmpty()) {
        //         requestId = UUID.randomUUID().toString();
        //     }
        //     
        //     // Agregar contexto al MDC
        //     MDC.put(REQUEST_ID, requestId);
        //     MDC.put(CLIENT_IP, getClientIp(httpRequest));
        //     MDC.put(USER_AGENT, httpRequest.getHeader("User-Agent"));
        //     MDC.put(REQUEST_URI, httpRequest.getRequestURI());
        //     MDC.put(REQUEST_METHOD, httpRequest.getMethod());
        //     
        //     // Recuperar usuario si esta autenticado
        //     String userId = httpRequest.getHeader("X-User-ID");
        //     if (userId != null && !userId.isEmpty()) {
        //         MDC.put(USER_ID, userId);
        //     }
        //     
        //     chain.doFilter(request, response);
        //     
        // } finally {
        //     // Limpiar MDC al finalizar
        //     MDC.clear();
        // }
        
        // Version sin MDC (temporal)
        chain.doFilter(request, response);
    }

    // TODO: Implementar metodo para obtener IP real del cliente
    // private String getClientIp(HttpServletRequest request) {
    //     String xForwardedFor = request.getHeader("X-Forwarded-For");
    //     if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
    //         return xForwardedFor.split(",")[0].trim();
    //     }
    //     return request.getRemoteAddr();
    // }
}
