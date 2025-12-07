package com.mutualidad.validacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ValidacionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidacionServiceApplication.class, args);
    }
}
