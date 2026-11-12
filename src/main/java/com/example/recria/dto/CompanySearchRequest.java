package com.example.recria.dto;

import jakarta.validation.constraints.Size;

public class CompanySearchRequest {
    @Size(max = 100)
    private String keyword;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
