package com.internance.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internance.common.kafka.event.EventEnvelope;
import com.internance.gateway.config.event.ConfigChangedEvent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.converter.ByteArrayJsonMessageConverter;
import org.springframework.messaging.Message;

/**
 * Locks in the wire contract with config-service: it publishes the {@code EventEnvelope}
 * as plain JSON with Jackson type-info disabled, so the gateway must recover
 * {@code EventEnvelope<ConfigChangedEvent>} purely from the listener method's generic
 * signature. This exercises the exact {@link ByteArrayJsonMessageConverter} wired by
 * {@link KafkaConsumerConfig}, without needing a broker.
 */
class ConfigEventDeserializationTest {

    // A record exactly as config-service emits it (type headers disabled, ISO-8601 instant).
    private static final String ENVELOPE_JSON = """
            {
              "eventId": "evt-123",
              "eventType": "config.changed",
              "occurredAt": "2026-06-07T10:15:30Z",
              "payload": {
                "application": "gateway-service",
                "label": "main",
                "paths": ["gateway-service/gateway-service.yml"],
                "commitId": "deadbeef"
              }
            }
            """;

    @Test
    @SuppressWarnings("unchecked")
    void deserializesEnvelopePayloadFromListenerGeneric() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ByteArrayJsonMessageConverter converter = new ByteArrayJsonMessageConverter(objectMapper);

        // The generic the converter resolves the payload against: EventEnvelope<ConfigChangedEvent>.
        Type payloadType = ConfigChangeListener.class
                .getMethod("onConfigChanged", EventEnvelope.class)
                .getGenericParameterTypes()[0];

        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "config-changed", 0, 0L, "gateway-service",
                ENVELOPE_JSON.getBytes(StandardCharsets.UTF_8));

        Message<?> message = converter.toMessage(record, null, null, payloadType);

        assertThat(message.getPayload()).isInstanceOf(EventEnvelope.class);
        EventEnvelope<ConfigChangedEvent> envelope = (EventEnvelope<ConfigChangedEvent>) message.getPayload();

        assertThat(envelope.eventId()).isEqualTo("evt-123");
        assertThat(envelope.eventType()).isEqualTo("config.changed");
        assertThat(envelope.occurredAt()).isNotNull();

        ConfigChangedEvent payload = envelope.payload();
        assertThat(payload).isNotNull();
        assertThat(payload.application()).isEqualTo("gateway-service");
        assertThat(payload.label()).isEqualTo("main");
        assertThat(payload.paths()).containsExactly("gateway-service/gateway-service.yml");
        assertThat(payload.commitId()).isEqualTo("deadbeef");
    }
}
