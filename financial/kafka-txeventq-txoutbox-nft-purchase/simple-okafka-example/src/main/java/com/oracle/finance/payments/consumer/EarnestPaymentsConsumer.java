// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.consumer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.finance.payments.model.EarnestPaymentEvent;
import com.oracle.finance.payments.service.EarnestPaymentsService;
import com.oracle.finance.payments.service.OverdraftException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

public class EarnestPaymentsConsumer implements Runnable, AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(EarnestPaymentsConsumer.class);

    private final KafkaConsumer<String, byte[]> consumer;
    private final Collection<String> topics;
    private final EarnestPaymentsService earnestPaymentsService;
    private boolean done = false;

    public EarnestPaymentsConsumer(@Qualifier("earnestPaymentsConsumer") KafkaConsumer<String, byte[]> consumer,
                                   @Qualifier("earnestPaymentsConsumerTopics") Collection<String> topics,
                                   EarnestPaymentsService earnestPaymentsService) {
        this.consumer = consumer;
        this.topics = topics;
        this.earnestPaymentsService = earnestPaymentsService;
    }

    @Override
    public void run() {
        consumer.subscribe(topics);
        while (!done) {
            try {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.of(5, ChronoUnit.SECONDS));
                if (!records.isEmpty()) {
                    processRecords(records);
                }
            } catch (Exception e) {
                logger.error("Error while consuming records", e);
            }
        }
        consumer.close();
    }

    private void processRecords(ConsumerRecords<String, byte[]> records) throws SQLException {
        Connection conn = consumer.getDBConnection();
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                EarnestPaymentEvent event = MAPPER.readValue(record.value(), EarnestPaymentEvent.class);
                earnestPaymentsService.processEarnestPayment(conn, event);
                logger.info("Processed EarnestPayment event: {}", event);
            } catch (OverdraftException oe) {
                logger.warn("Earnest Payment Overdraft: {}", oe.toString());
                earnestPaymentsService.processOverdraft(conn, oe);
            } catch (IOException je) {
                logger.error("Invalid event: {}", je.toString());
            }
        }
        consumer.commitSync();
        logger.info("Committed records: {}", records.count());
    }

    @Override
    public void close() throws Exception {
        done = true;
    }
}
