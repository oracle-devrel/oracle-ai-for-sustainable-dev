package com.oracle.demo.deepsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeepDataSecurityProperties.class)
public class DeepDataSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepDataSecurityApplication.class, args);
    }
}
