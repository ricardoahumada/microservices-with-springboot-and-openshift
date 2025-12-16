package com.mutualidad.afiliado.api.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test/resiliencia")
@RequiredArgsConstructor
public class ResilienciaTestController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/circuit-breakers")
    public Map<String, String> getCircuitBreakersStatus() {
        return circuitBreakerRegistry.getAllCircuitBreakers().toJavaStream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().name()
            ));
    }

    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String name) {
        circuitBreakerRegistry.circuitBreaker(name).reset();
        return ResponseEntity.ok("Circuit breaker " + name + " reseteado");
    }
}
