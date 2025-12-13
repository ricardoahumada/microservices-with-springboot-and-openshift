package com.mutualidad.afiliado.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // TODO Ejercicio 1: Configurar el topic de eventos de afiliado
    // 
    // Pasos:
    // 1. Inyectar el nombre del topic desde application.yml usando @Value
    //    @Value("${app.kafka.topic.afiliado-eventos}")
    //    private String afiliadoEventosTopic;
    //
    // 2. Crear un Bean NewTopic con:
    //    - Nombre: afiliadoEventosTopic
    //    - Particiones: 3 (permite paralelismo)
    //    - Replicas: 1 (desarrollo local)
    //
    // Ejemplo:
    // @Bean
    // public NewTopic afiliadoEventosTopic() {
    //     return TopicBuilder.name(afiliadoEventosTopic)
    //             .partitions(3)
    //             .replicas(1)
    //             .build();
    // }
}
