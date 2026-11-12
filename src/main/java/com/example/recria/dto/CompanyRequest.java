
package com.example.recria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompanyRequest {

    @NotBlank
    @Size(max = 100)
    private String companyName;

    @Size(max = 1000)
    private String companyInfo;

    @Size(max = 1000)
    private String companyMemo;

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
}

