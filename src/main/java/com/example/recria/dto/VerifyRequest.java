package com.example.recria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "認証コードは半角数字6桁で入力してください")
    private String code;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}