package com.payments.system.container.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class PaymentSystemConfiguration {

    public static final String PAYMENTS_COMPLETED_TOPIC = "payments-completed-topic";

    @Bean
    public NewTopic paymentsCompletedTopic() {
        return TopicBuilder.name(PAYMENTS_COMPLETED_TOPIC).partitions(1).replicas(1).config(TopicConfig.RETENTION_MS_CONFIG, "86400000").config(TopicConfig.RETENTION_BYTES_CONFIG, "524288000").build();
    }
}
