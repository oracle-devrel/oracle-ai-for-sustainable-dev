// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.configuration;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = OKafkaProperties.PREFIX)
public class OKafkaProperties {
    public static final String PREFIX = "okafka";

    private String consumerGroup;
    private int consumerThreads;
    private String oracleServiceName;
    private String ojdbcPath;
    private String securityProtocol;
    private String bootstrapServers;
    private List<TopicProperties> topics;

    public int getConsumerThreads() {
        return consumerThreads;
    }

    public void setConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getOracleServiceName() {
        return oracleServiceName;
    }

    public void setOracleServiceName(String oracleServiceName) {
        this.oracleServiceName = oracleServiceName;
    }

    public String getOjdbcPath() {
        return ojdbcPath;
    }

    public void setOjdbcPath(String ojdbcPath) {
        this.ojdbcPath = ojdbcPath;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public List<TopicProperties> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicProperties> topics) {
        this.topics = topics;
    }

    public Properties createConnectionProperties() {
        Properties props = new Properties();
        props.put("security.protocol", securityProtocol);
        if (securityProtocol.equalsIgnoreCase("PLAINTEXT")) {
            props.put("oracle.service.name", oracleServiceName);
            props.put("bootstrap.servers", bootstrapServers);
            // If using Oracle Database wallet, pass wallet directory
            props.put("oracle.net.tns_admin", ojdbcPath);
        } else {
            props.put("oracle.service.name", oracleServiceName);
            props.put("tns.alias", oracleServiceName);
            // If using Oracle Database wallet, pass wallet directory
            props.put("oracle.net.tns_admin", ojdbcPath);
        }
        return props;
    }

    public List<String> getTopicNames() {
        return getTopics().stream()
                .map(OKafkaProperties.TopicProperties::getName)
                .toList();
    }

    public List<NewTopic> getNewTopics() {
        return getTopics().stream()
                .map(t -> new NewTopic(t.name, t.partitions, (short) 0))
                .toList();
    }

    public static class TopicProperties {
        private String name;
        private int partitions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof TopicProperties that)) return false;

            return getPartitions() == that.getPartitions() && Objects.equals(getName(), that.getName());
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(getName());
            result = 31 * result + getPartitions();
            return result;
        }
    }
}
