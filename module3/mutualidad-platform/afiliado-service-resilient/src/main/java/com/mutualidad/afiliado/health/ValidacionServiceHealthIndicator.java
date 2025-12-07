package com.mutualidad.afiliado.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component("validacionService")
public class ValidacionServiceHealthIndicator implements HealthIndicator {

    @Value("${services.validacion.url}")
    private String validacionServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {
        try {
            String statusUrl = validacionServiceUrl + "/api/validaciones/status";
            String response = restTemplate.getForObject(statusUrl, String.class);
            
            return Health.up()
                    .withDetail("service", "validacion-service")
                    .withDetail("url", validacionServiceUrl)
                    .withDetail("status", response)
                    .build();
        } catch (Exception e) {
            log.warn("Validacion service health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "validacion-service")
                    .withDetail("url", validacionServiceUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
