package com.example.recria.entity;

import java.time.OffsetDateTime;

public class Reflection {
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getReflectionText() {
        return reflectionText;
    }

    public void setReflectionText(String reflectionText) {
        this.reflectionText = reflectionText;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private Long eventId;
    private String reflectionText;
    private OffsetDateTime updatedAt;

}