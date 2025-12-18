package com.mutualidad.afiliado.filter;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para agregar contexto de auditoria al MDC (Mapped Diagnostic Context).
 * Permite trazabilidad de peticiones en logs estructurados.
 */
@Component
@Order(1)
public class AuditMDCFilter implements Filter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_AGENT = "userAgent";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // Generar o recuperar Request ID
            String requestId = httpRequest.getHeader("X-Request-ID");
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            
            // Agregar contexto al MDC
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, getClientIp(httpRequest));
            MDC.put(USER_AGENT, httpRequest.getHeader("User-Agent"));
            MDC.put(REQUEST_URI, httpRequest.getRequestURI());
            MDC.put(REQUEST_METHOD, httpRequest.getMethod());
            
            // Recuperar usuario si esta autenticado
            String userId = httpRequest.getHeader("X-User-ID");
            if (userId != null && !userId.isEmpty()) {
                MDC.put(USER_ID, userId);
            }
            
            chain.doFilter(request, response);
            
        } finally {
            // Limpiar MDC al finalizar
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
