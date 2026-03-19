package com.event.registration.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {
    private Long registrantId;
    private Long eventId;
    private String paymentMode; // RAZORPAY, MANUAL
    private String registrantEmail;
}
