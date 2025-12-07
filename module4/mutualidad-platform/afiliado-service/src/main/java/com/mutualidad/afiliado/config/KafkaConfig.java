package com.mutualidad.afiliado.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.afiliado-eventos}")
    private String afiliadoEventosTopic;

    @Bean
    public NewTopic afiliadoEventosTopic() {
        return TopicBuilder.name(afiliadoEventosTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
