package com.acme.insurance.pas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;

@Configuration
public class JmsConfig {

    private static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDeliveryPersistent(true);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public JmsTemplate noOpJmsTemplate() {
        log.info("JMS ConnectionFactory not available; creating no-op JmsTemplate stub");
        return new NoOpJmsTemplate();
    }

    private static class NoOpJmsTemplate extends JmsTemplate {
        @Override
        public void convertAndSend(String destinationName, Object message) {
            LoggerFactory.getLogger(NoOpJmsTemplate.class)
                    .debug("No-op JMS send to {}: {}", destinationName, message);
        }

        @Override
        public void afterPropertiesSet() {
            // Skip ConnectionFactory validation
        }
    }
}
