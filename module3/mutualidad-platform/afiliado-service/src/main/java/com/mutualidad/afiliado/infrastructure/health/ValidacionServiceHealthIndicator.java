package com.mutualidad.afiliado.infrastructure.health;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ValidacionServiceHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;

    @Value("${services.validacion.url}")
    private String validacionUrl;

    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                validacionUrl + "/api/v1/actuator/health", String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("service", "validacion-service")
                    .withDetail("url", validacionUrl)
                    .build();
            }
            return Health.down()
                .withDetail("service", "validacion-service")
                .withDetail("status", response.getStatusCode().toString())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "validacion-service")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
