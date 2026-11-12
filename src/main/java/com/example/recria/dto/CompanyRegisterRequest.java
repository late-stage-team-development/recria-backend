package com.example.recria.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class CompanyRegisterRequest {
    @NotBlank
    @Size(max = 100)
    private String companyName;

    @Size(max = 1000)
    private String companyInfo;

    @Size(max = 1000)
    private String companyMemo;

    @NotBlank
    @Size(max = 100)
    private String eventName;

    @NotNull
    @Future
    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    @Future
    private OffsetDateTime notifyAt;

    @Size(max = 1000)
    private String eventMemo;

    @Size(max = 20)
    private String eventTag;

    @JsonIgnore
    @AssertTrue(message = "終了日時は開始日時以降に設定してください")
    public boolean isEndAtValid() {
        return endAt == null || startAt == null || !endAt.isBefore(startAt);
    }

    @JsonIgnore
    @AssertTrue(message = "通知日時は現在より未来、かつ開始日時より前に設定してください")
    public boolean isNotifyAtValid() {
        return notifyAt == null || startAt == null || notifyAt.isBefore(startAt);
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyInfo() { return companyInfo; }
    public void setCompanyInfo(String companyInfo) { this.companyInfo = companyInfo; }
    public String getCompanyMemo() { return companyMemo; }
    public void setCompanyMemo(String companyMemo) { this.companyMemo = companyMemo; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }
    public OffsetDateTime getNotifyAt() { return notifyAt; }
    public void setNotifyAt(OffsetDateTime notifyAt) { this.notifyAt = notifyAt; }
    public String getEventMemo() { return eventMemo; }
    public void setEventMemo(String eventMemo) { this.eventMemo = eventMemo; }
    public String getEventTag() { return eventTag; }
    public void setEventTag(String eventTag) { this.eventTag = eventTag; }
}
