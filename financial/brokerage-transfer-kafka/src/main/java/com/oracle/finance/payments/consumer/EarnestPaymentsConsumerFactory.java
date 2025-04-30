// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.oracle.finance.payments.configuration.OKafkaProperties;
import com.oracle.finance.payments.service.EarnestPaymentsService;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Creates and starts {@link com.oracle.finance.payments.consumer.EarnestPaymentsConsumer} instances
 * as Virtual Threads after application startup
 */
@Component
public class EarnestPaymentsConsumerFactory implements DisposableBean {
    private final OKafkaProperties okafkaProperties;
    private final EarnestPaymentsService earnestPaymentsService;
    private final AsyncTaskExecutor executor;
    private final List<EarnestPaymentsConsumer> consumers = new ArrayList<>();

    public EarnestPaymentsConsumerFactory(OKafkaProperties okafkaProperties,
                                          EarnestPaymentsService earnestPaymentsService,
                                          @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor) {
        this.okafkaProperties = okafkaProperties;
        this.earnestPaymentsService = earnestPaymentsService;
        this.executor = executor;
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < okafkaProperties.getConsumerThreads(); i++) {
            EarnestPaymentsConsumer consumer = createConsumer();
            executor.submit(consumer);
            consumers.add(consumer);
        }
    }

    public EarnestPaymentsConsumer createConsumer() {
        return new EarnestPaymentsConsumer(
                kafkaConsumer(),
                okafkaProperties.getTopicNames(),
                earnestPaymentsService
        );
    }

    private KafkaConsumer<String, byte[]> kafkaConsumer() {
        Properties props = okafkaProperties.createConnectionProperties();
        props.put("group.id", okafkaProperties.getConsumerGroup());
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", 100);
        props.put("auto.offset.reset", "earliest");

        Deserializer<String> keyDeserializer = new StringDeserializer();
        return new KafkaConsumer<>(props, keyDeserializer, new ByteArrayDeserializer());
    }

    @Override
    public void destroy() throws Exception {
        for (EarnestPaymentsConsumer consumer : consumers) {
            consumer.close();
        }
    }
}
