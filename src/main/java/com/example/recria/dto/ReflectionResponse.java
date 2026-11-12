package com.example.recria.dto;

import java.time.OffsetDateTime;

public class ReflectionResponse {
    private Long eventId;
    private String reflectionText;
    private String result;
    private OffsetDateTime updatedAt;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getReflectionText() { return reflectionText; }
    public void setReflectionText(String reflectionText) { this.reflectionText = reflectionText; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
