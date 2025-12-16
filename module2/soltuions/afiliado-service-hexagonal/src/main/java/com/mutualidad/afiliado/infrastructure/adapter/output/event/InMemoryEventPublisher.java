package com.mutualidad.afiliado.infrastructure.adapter.output.event;

import com.mutualidad.afiliado.application.port.output.EventPublisherPort;
import com.mutualidad.afiliado.domain.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class InMemoryEventPublisher implements EventPublisherPort {

    private final List<AfiliadoEvent> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(AfiliadoEvent event) {
        log.info("Publicando evento: {} para afiliado: {}", 
            event.getTipoEvento(), event.getAfiliadoId());
        publishedEvents.add(event);
    }

    @Override
    public void publishAll(Iterable<AfiliadoEvent> events) {
        events.forEach(this::publish);
    }

    public List<AfiliadoEvent> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
