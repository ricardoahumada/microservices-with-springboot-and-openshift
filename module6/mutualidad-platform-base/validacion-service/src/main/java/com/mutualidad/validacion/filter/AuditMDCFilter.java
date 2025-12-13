package com.mutualidad.validacion.filter;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * TODO Ejercicio 1.3: Implementar filtro MDC para contexto de negocio
 * Ver afiliado-service/filter/AuditMDCFilter.java para ejemplo completo.
 */
@Component
@Order(1)
public class AuditMDCFilter implements Filter {

    // TODO: Implementar igual que en afiliado-service

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // TODO: Agregar logica MDC
        chain.doFilter(request, response);
    }
}
