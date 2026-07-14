package com.oracle.demo.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ObservabilityApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(ObservabilityApplication.class, args);
  }

  @Bean
  ApplicationRunner registerGlobalOpenTelemetry(OpenTelemetry openTelemetry) {
    return args -> {
      try {
        GlobalOpenTelemetry.set(openTelemetry);
        LOGGER.info("Registered Spring Boot OpenTelemetry instance as GlobalOpenTelemetry for Oracle JDBC providers");
      } catch (IllegalStateException e) {
        LOGGER.warn("GlobalOpenTelemetry was already initialized before startup registration; Oracle JDBC provider spans may use that instance");
      }
    };
  }
}
