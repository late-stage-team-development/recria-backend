package com.example.recria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ReflectionRequest {
    @NotBlank
    @Size(max = 5000)
    private String reflectionText;

    @Pattern(regexp = "未定|合格|不合格", message = "結果は「未定」「合格」「不合格」のいずれかを指定してください")
    private String result;

    public String getReflectionText() { return reflectionText; }
    public void setReflectionText(String reflectionText) { this.reflectionText = reflectionText; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}