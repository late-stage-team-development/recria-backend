package com.example.recria.dto;

import java.time.OffsetDateTime;

public class EventResponse {
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public OffsetDateTime getNotifyAt() {
        return notifyAt;
    }

    public void setNotifyAt(OffsetDateTime notifyAt) {
        this.notifyAt = notifyAt;
    }

    public String getEventMemo() {
        return eventMemo;
    }

    public void setEventMemo(String eventMemo) {
        this.eventMemo = eventMemo;
    }

    public String getEventTag() {
        return eventTag;
    }

    public void setEventTag(String eventTag) {
        this.eventTag = eventTag;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    private Long eventId;
    private Long companyId;
    private String companyName;
    private String eventName;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private OffsetDateTime notifyAt;
    private String eventMemo;
    private String eventTag;
    private String result;

}