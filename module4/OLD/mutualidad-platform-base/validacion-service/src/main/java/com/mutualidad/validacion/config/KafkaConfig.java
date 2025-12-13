package com.mutualidad.validacion.config;

import org.springframework.context.annotation.Configuration;

// TODO: Importar clases para configurar DLQ (Dead Letter Queue)
// import org.apache.kafka.clients.consumer.ConsumerConfig;
// import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
// import org.springframework.kafka.core.*;
// import org.springframework.kafka.listener.DefaultErrorHandler;
// import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
// import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración del Consumer de Kafka con manejo de errores.
 * 
 * Objetivo avanzado: Implementar Dead Letter Queue (DLQ)
 * 
 * Cuando un mensaje falla después de N reintentos, 
 * se envía automáticamente a un topic .dlt (Dead Letter Topic)
 */
@Configuration
public class KafkaConfig {

    /**
     * TODO AVANZADO: Configurar error handler con DLQ
     * 
     * Pasos:
     * 1. Crear DeadLetterPublishingRecoverer con KafkaTemplate
     * 2. Crear DefaultErrorHandler con backoff (ej: 3 reintentos, 1s intervalo)
     * 3. Configurar en el container factory
     * 
     * Ejemplo:
     * DeadLetterPublishingRecoverer recoverer = 
     *     new DeadLetterPublishingRecoverer(kafkaTemplate);
     * DefaultErrorHandler errorHandler = 
     *     new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
     */
    
    // @Bean
    // public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
    //         ConsumerFactory<String, String> consumerFactory,
    //         KafkaTemplate<String, String> kafkaTemplate) {
    //     
    //     ConcurrentKafkaListenerContainerFactory<String, String> factory = 
    //         new ConcurrentKafkaListenerContainerFactory<>();
    //     factory.setConsumerFactory(consumerFactory);
    //     
    //     // TODO: Configurar error handler con DLQ
    //     // DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
    //     // DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    //     // factory.setCommonErrorHandler(errorHandler);
    //     
    //     return factory;
    // }
}
