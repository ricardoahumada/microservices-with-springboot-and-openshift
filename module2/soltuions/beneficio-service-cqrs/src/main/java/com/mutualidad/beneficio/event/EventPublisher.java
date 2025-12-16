package com.mutualidad.beneficio.event;

public interface EventPublisher {
    void publish(BeneficioEvent event);
}
