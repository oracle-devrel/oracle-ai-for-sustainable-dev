package com.oracle.demo.lakehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LakehouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LakehouseApplication.class, args);
    }
}
