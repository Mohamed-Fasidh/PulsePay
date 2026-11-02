package com.pulsepay.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies the local-development event and cache images can start in CI with Docker available. */
@Testcontainers(disabledWithoutDocker = true)
class MessagingContainersTest {
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"));

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());

    @Test
    void startsKafkaAndRedisForEventAndRateLimitIntegrationTests() {
        assertTrue(KAFKA.isRunning());
        assertTrue(REDIS.isRunning());
    }
}
