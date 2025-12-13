package com.mutualidad.afiliado.config;

// TODO: Importar clases necesarias de Kafka
// import org.apache.kafka.clients.admin.NewTopic;
// import org.apache.kafka.clients.producer.ProducerConfig;
// import org.apache.kafka.common.serialization.StringSerializer;
// import org.springframework.kafka.core.*;
// import org.springframework.kafka.support.serializer.JsonSerializer;

import org.springframework.context.annotation.Configuration;

/**
 * Configuración del Producer de Kafka.
 * 
 * Objetivos:
 * 1. Configurar ProducerFactory con serializers
 * 2. Crear KafkaTemplate para enviar mensajes
 * 3. Crear el topic "afiliado-eventos"
 */
@Configuration
public class KafkaConfig {

    // TODO: Inyectar bootstrap-servers desde application.yml
    // @Value("${spring.kafka.bootstrap-servers}")
    // private String bootstrapServers;

    /**
     * TODO: Configurar ProducerFactory
     * 
     * Propiedades requeridas:
     * - BOOTSTRAP_SERVERS_CONFIG: Dirección del broker Kafka
     * - KEY_SERIALIZER_CLASS_CONFIG: StringSerializer
     * - VALUE_SERIALIZER_CLASS_CONFIG: JsonSerializer
     * - ACKS_CONFIG: "all" para garantizar durabilidad
     * - ENABLE_IDEMPOTENCE_CONFIG: true para evitar duplicados
     */
    // @Bean
    // public ProducerFactory<String, AfiliadoEvent> producerFactory() {
    //     Map<String, Object> props = new HashMap<>();
    //     // TODO: Configurar propiedades
    //     return new DefaultKafkaProducerFactory<>(props);
    // }

    /**
     * TODO: Crear KafkaTemplate
     * 
     * El template se usa para enviar mensajes al topic.
     */
    // @Bean
    // public KafkaTemplate<String, AfiliadoEvent> kafkaTemplate() {
    //     return new KafkaTemplate<>(producerFactory());
    // }

    /**
     * TODO: Crear el topic "afiliado-eventos"
     * 
     * Parámetros:
     * - nombre: "afiliado-eventos"
     * - particiones: 3 (para paralelismo)
     * - factor de replicación: 1 (desarrollo)
     */
    // @Bean
    // public NewTopic afiliadoEventosTopic() {
    //     return new NewTopic("afiliado-eventos", 3, (short) 1);
    // }
}
