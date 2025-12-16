package com.mutualidad.afiliado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AfiliadoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfiliadoServiceApplication.class, args);
    }
}
