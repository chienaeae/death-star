package com.deathstar.vader.event.domain;

import io.nats.client.Message;
import java.time.Duration;

public record EventMessage(DomainEvent domainEvent, Message rawMessage) {
    public void ack() {
        if (rawMessage != null) {
            rawMessage.ack();
        }
    }

    public void nack() {
        if (rawMessage != null) {
            rawMessage.nak();
        }
    }

    public void nackWithDelay(Duration delay) {
        if (rawMessage != null) {
            rawMessage.nakWithDelay(delay);
        }
    }
}
