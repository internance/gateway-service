package com.internance.gateway.config;

import com.internance.common.kafka.event.EventEnvelope;
import com.internance.gateway.config.event.ConfigChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.context.refresh.ContextRefresher;

import java.time.Instant;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConfigChangeListenerTest {

    private static final String APPLICATION = "gateway-service";

    @Mock
    ContextRefresher contextRefresher;

    ConfigChangeListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConfigChangeListener(contextRefresher, APPLICATION);
    }

    @Test
    void refreshesWhenEventTargetsThisApplication() {
        given(contextRefresher.refresh()).willReturn(Set.of("jwt.secret"));

        listener.onConfigChanged(envelopeFor(APPLICATION));

        verify(contextRefresher).refresh();
    }

    @Test
    void ignoresEventForAnotherApplication() {
        listener.onConfigChanged(envelopeFor("auth-service"));

        verifyNoInteractions(contextRefresher);
    }

    @Test
    void ignoresEventWithNullPayload() {
        EventEnvelope<ConfigChangedEvent> envelope =
                new EventEnvelope<>("evt-1", ConfigChangedEvent.class.getSimpleName(), Instant.now(), null);

        listener.onConfigChanged(envelope);

        verifyNoInteractions(contextRefresher);
    }

    private static EventEnvelope<ConfigChangedEvent> envelopeFor(String application) {
        ConfigChangedEvent payload = new ConfigChangedEvent(
                application, "main", Set.of(application + "/" + application + ".yml"), "deadbeef");
        return EventEnvelope.of("config.changed", payload);
    }
}
