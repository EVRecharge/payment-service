package com.evcharging.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayoutAccountResponse {
    private String status;
    private String beneficiaryName;
    private String bankAccountLast4;
    private String ifsc;
    private String panLast4;
    private String statusReason;
}
