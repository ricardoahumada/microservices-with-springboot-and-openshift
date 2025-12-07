package com.mutualidad.beneficio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BeneficioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeneficioServiceApplication.class, args);
    }
}
