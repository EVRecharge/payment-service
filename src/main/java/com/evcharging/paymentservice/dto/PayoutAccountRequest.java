package com.evcharging.paymentservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class PayoutAccountRequest {
    @NotBlank
    private String beneficiaryName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6,20}$", message = "Enter a valid bank account number")
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC code")
    private String ifsc;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Enter a valid PAN")
    private String pan;

    @NotBlank
    private String businessType;

    @NotBlank
    private String email;

    @NotBlank
    private String phone;
}
