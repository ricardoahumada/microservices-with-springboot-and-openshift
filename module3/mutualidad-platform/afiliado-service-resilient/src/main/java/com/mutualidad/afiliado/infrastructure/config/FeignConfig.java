package com.mutualidad.afiliado.infrastructure.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5, TimeUnit.SECONDS,  // connectTimeout
            5, TimeUnit.SECONDS,  // readTimeout
            true                   // followRedirects
        );
    }

    @Bean
    public Retryer retryer() {
        // Deshabilitamos el retryer de Feign porque usamos Resilience4j
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign error - method: {}, status: {}", methodKey, response.status());
            
            if (response.status() >= 400 && response.status() < 500) {
                return new BusinessException("Error de cliente: " + response.status());
            }
            
            return defaultDecoder.decode(methodKey, response);
        }
    }

    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
