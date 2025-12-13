package com.mutualidad.afiliado.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = generateCorrelationId();
            log.debug("Generando nuevo Correlation ID: {}", correlationId);
        } else {
            log.debug("Usando Correlation ID recibido: {}", correlationId);
        }

        // Agregar al MDC para logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Agregar a la respuesta
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String getCurrentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        return correlationId != null ? correlationId : "no-correlation-id";
    }
}
