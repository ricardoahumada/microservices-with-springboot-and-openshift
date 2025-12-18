package com.mutualidad.validacion.config;

import com.mutualidad.validacion.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    // TODO Ejercicio 5: Inyectar nombre del topic DLT
    @Value("${app.kafka.topic.afiliado-eventos-dlt}")
    private String dltTopic;

    /**
     * TODO Ejercicio 5: Crear el topic para Dead Letter Queue
     * 
     * El DLT almacena mensajes que fallaron despues de los reintentos.
     * Convencion: nombre del topic original + ".dlt"
     */
    @Bean
    public NewTopic dltTopic() {
        return TopicBuilder.name(dltTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * TODO Ejercicio 5: Configurar el error handler con DLQ
     * 
     * Pasos:
     * 1. Crear DeadLetterPublishingRecoverer que envia mensajes fallidos al DLT
     * 2. Crear DefaultErrorHandler con:
     *    - El recoverer creado
     *    - FixedBackOff para reintentos (ej: 3 reintentos, 1 segundo entre cada uno)
     * 3. Opcionalmente agregar RetryListener para loguear cada reintento
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, AfiliadoEvent> kafkaTemplate) {
        // Configura DLQ: envia mensajes fallidos al topic .dlt
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> {
                log.error("Enviando mensaje fallido a DLT: key={}, error={}", 
                    record.key(), ex.getMessage());
                return new org.apache.kafka.common.TopicPartition(dltTopic, 0);
            });
    
        // Reintentar 3 veces con intervalo de 1 segundo antes de enviar a DLT
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, 
            new FixedBackOff(1000L, 3));
        
        // Log de cada reintento
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Reintento {} de 3 para mensaje: key={}, error={}", 
                deliveryAttempt, ((ConsumerRecord<?, ?>)record).key(), ex.getMessage());
        });
    
        return errorHandler;
    }
}
