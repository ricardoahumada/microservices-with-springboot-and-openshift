package com.mutualidad.beneficio.infrastructure.event;

import com.mutualidad.beneficio.event.BeneficioEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InMemoryEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public InMemoryEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(BeneficioEvent event) {
        log.info("Publicando evento: {} para beneficio: {}", event.getTipoEvento(), event.getAggregateId());
        applicationEventPublisher.publishEvent(event);
    }
}
