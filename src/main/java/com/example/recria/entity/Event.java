package com.example.recria.entity;

import java.time.OffsetDateTime;

public class Event {
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public String getEventTag() {
        return eventTag;
    }

    public void setEventTag(String eventTag) {
        this.eventTag = eventTag;
    }

    public String getEventMemo() {
        return eventMemo;
    }

    public void setEventMemo(String eventMemo) {
        this.eventMemo = eventMemo;
    }

    public OffsetDateTime getNotifyAt() {
        return notifyAt;
    }

    public void setNotifyAt(OffsetDateTime notifyAt) {
        this.notifyAt = notifyAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private Long eventId;
    private Long companyId;
    private String eventName;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String eventTag;
    private String eventMemo;
    private OffsetDateTime notifyAt;
    private String result;
    private OffsetDateTime createdAt;

}