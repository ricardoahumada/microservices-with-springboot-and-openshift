package com.mutualidad.afiliado.event;

import java.time.LocalDateTime;

/**
 * DTO que representa un evento de afiliado para publicar en Kafka.
 * 
 * TODO: Completar los campos del evento:
 * - eventId: Identificador único del evento (UUID)
 * - eventType: Tipo de evento (AFILIADO_CREATED, AFILIADO_UPDATED, AFILIADO_DELETED)
 * - afiliadoId: ID del afiliado
 * - dni, nombre, apellidos, email: Datos del afiliado
 * - timestamp: Momento de creación del evento
 */
public class AfiliadoEvent {
    
    // TODO: Definir los campos del evento
    // private String eventId;
    // private String eventType;
    // private String afiliadoId;
    // private String dni;
    // private String nombre;
    // private String apellidos;
    // private String email;
    // private LocalDateTime timestamp;

    public AfiliadoEvent() {}

    // TODO: Crear constructor con parámetros
    // public AfiliadoEvent(String eventType, String dni, String nombre, 
    //                      String apellidos, String email) {
    //     this.eventId = UUID.randomUUID().toString();
    //     this.eventType = eventType;
    //     ...
    //     this.timestamp = LocalDateTime.now();
    // }

    // TODO: Generar getters y setters para todos los campos

    @Override
    public String toString() {
        // TODO: Implementar toString()
        return "AfiliadoEvent{}";
    }
}
