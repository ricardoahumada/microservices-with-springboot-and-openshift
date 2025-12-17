package com.mutualidad.afiliado.infrastructure.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry registry;

    @Override
    public Health health() {
        Map<String, String> states = registry.getAllCircuitBreakers().toJavaStream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().name()
            ));

        boolean allClosed = states.values().stream()
            .allMatch(state -> "CLOSED".equals(state));

        Health.Builder builder = allClosed ? Health.up() : Health.down();
        return builder.withDetails(states).build();
    }
}
