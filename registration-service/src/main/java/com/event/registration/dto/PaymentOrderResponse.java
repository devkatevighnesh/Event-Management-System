package com.event.registration.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * Returned by POST /registration/payment/order
 * Frontend uses razorpayKeyId + razorpayOrderId to open Razorpay Checkout modal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponse {
    private Long registrationId;
    private String ticketNo;
    private String razorpayOrderId;
    private String razorpayKeyId;    // Key to pass to Razorpay Checkout SDK
    private BigDecimal amount;
    private String currency;
}
