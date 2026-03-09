package com.civic.crowdcivics.dto;

import java.time.LocalDateTime;

public class OTPStatusResponse {
    private String email;
    private String otp;
    private String purpose;
    private LocalDateTime expiryDate;
    private boolean used;
    private boolean expired;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;

    }

    public String getOtp() {
        return otp;
    }
    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPurpose() {
        return purpose;
    }
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }
    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isExpired() {
        return expired;
    }
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}