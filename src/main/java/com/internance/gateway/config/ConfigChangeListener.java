package com.internance.gateway.config;

import com.internance.common.kafka.event.EventEnvelope;
import com.internance.gateway.config.event.ConfigChangedEvent;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to the {@code config.changed} events config-service publishes through
 * common-lib's {@code EventPublisher}, and refreshes this service's configuration when
 * one targets {@code gateway-service}.
 *
 * <p>The envelope is deserialized as {@code EventEnvelope<ConfigChangedEvent>} (see
 * {@link KafkaConsumerConfig}). Every gateway instance must react to a change, so the
 * consumer group is made unique per instance ({@code spring.application.name}-{uuid} in
 * {@code application.yml}) — each instance forms its own group and receives every record,
 * giving broadcast semantics rather than the single-consumer split a shared group would.
 *
 * <p>On a matching event we call {@link ContextRefresher#refresh()}, which re-pulls the
 * application's config from config-service and rebinds {@code @ConfigurationProperties}
 * beans (and republishes gateway routes), so the new values take effect without a restart.
 */
@Slf4j
@Component
public class ConfigChangeListener {

    private final ContextRefresher contextRefresher;
    private final String applicationName;

    public ConfigChangeListener(ContextRefresher contextRefresher,
                                @Value("${spring.application.name}") String applicationName) {
        this.contextRefresher = contextRefresher;
        this.applicationName = applicationName;
    }

    @KafkaListener(topics = "${config.events.topic}")
    public void onConfigChanged(EventEnvelope<ConfigChangedEvent> envelope) {
        ConfigChangedEvent event = envelope.payload();
        if (event == null || !applicationName.equals(event.application())) {
            log.debug("Ignoring config change event {} for application={}",
                    envelope.eventId(), event == null ? null : event.application());
            return;
        }

        log.info("Config changed for {} (label={}, commit={}, paths={}); refreshing context",
                event.application(), event.label(), event.commitId(), event.paths());
        Set<String> changedKeys = contextRefresher.refresh();
        log.info("Context refresh complete for {}: {} propert{} changed",
                applicationName, changedKeys.size(), changedKeys.size() == 1 ? "y" : "ies");
    }
}
