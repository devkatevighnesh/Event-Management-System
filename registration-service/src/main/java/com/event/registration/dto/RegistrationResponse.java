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
    private Long eventId;
    private String ticketNo;
    private String status;
    private String razorpayOrderId;  // returned so frontend knows the orderId after STEP 1 (if applicable)
    private BigDecimal amount;
}
