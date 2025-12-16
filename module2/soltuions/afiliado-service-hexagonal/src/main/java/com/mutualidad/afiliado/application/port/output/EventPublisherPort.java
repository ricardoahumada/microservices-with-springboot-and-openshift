package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.event.AfiliadoEvent;

public interface EventPublisherPort {

    void publish(AfiliadoEvent event);

    void publishAll(Iterable<AfiliadoEvent> events);
}
