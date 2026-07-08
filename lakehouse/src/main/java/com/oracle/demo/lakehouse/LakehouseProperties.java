package com.oracle.demo.lakehouse;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lakehouse.iceberg")
public record LakehouseProperties(
        String tableName,
        int sampleLimit,
        int queryTimeoutSeconds) {

    public LakehouseProperties {
        if (sampleLimit <= 0) {
            sampleLimit = 25;
        }
        if (queryTimeoutSeconds <= 0) {
            queryTimeoutSeconds = 30;
        }
    }
}
