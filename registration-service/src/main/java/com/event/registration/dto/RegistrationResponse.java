package com.event.registration.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponse {
    private Long registrationId;
    private String ticketNo;
    private String status;
    private String razorpayOrderId;
    private BigDecimal amount;
}
