// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.configuration;

import java.util.concurrent.ExecutionException;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.errors.TopicExistsException;
import org.oracle.okafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * TopicFactory creates any necessary topics for the Application.
 */
@Component
public class TopicFactory {
    private static final Logger logger = LoggerFactory.getLogger(TopicFactory.class);

    private final OKafkaProperties okafkaProperties;

    public TopicFactory(OKafkaProperties okafkaProperties) {
        this.okafkaProperties = okafkaProperties;
    }

    @PostConstruct
    public void init() {
        try (Admin admin = AdminClient.create(okafkaProperties.createConnectionProperties())) {
            admin.createTopics(okafkaProperties.getNewTopics())
                    .all()
                    .get();
            logger.info("Topics created");
        } catch (ExecutionException | InterruptedException e) {
            // If the topic already exists, drop the exception.
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException(e);
            } else {
                logger.info("Skipped topic creation, topics already exist");
            }
        }
    }
}
