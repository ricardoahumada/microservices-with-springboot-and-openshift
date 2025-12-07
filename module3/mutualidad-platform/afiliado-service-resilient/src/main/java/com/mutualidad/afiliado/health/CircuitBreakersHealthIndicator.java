package com.mutualidad.afiliado.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("circuitBreakers")
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            CircuitBreaker.State state = cb.getState();
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", state.name());
            cbDetails.put("failureRate", cb.getMetrics().getFailureRate() + "%");
            cbDetails.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            
            details.put(cb.getName(), cbDetails);

            // Si algun CB esta OPEN, el servicio no esta completamente sano
            if (state == CircuitBreaker.State.OPEN) {
                allHealthy = false;
            }
        }

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        
        return builder
                .withDetails(details)
                .build();
    }
}
