package com.mutualidad.notificacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NotificacionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacionServiceApplication.class, args);
    }
}
