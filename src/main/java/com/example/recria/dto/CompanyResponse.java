
package com.example.recria.dto;

import java.util.List;

public class CompanyResponse {

    private Long companyId;
    private String companyName;
    private String companyInfo;
    private String companyMemo;
    private List<EventResponse> events;

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

    public String getCompanyInfo() {
        return companyInfo;
    }

    public void setCompanyInfo(String companyInfo) {
        this.companyInfo = companyInfo;
    }

    public String getCompanyMemo() {
        return companyMemo;
    }

    public void setCompanyMemo(String companyMemo) {
        this.companyMemo = companyMemo;
    }

    public List<EventResponse> getEvents() {
        return events;
    }

    public void setEvents(List<EventResponse> events) {
        this.events = events;
    }
}

