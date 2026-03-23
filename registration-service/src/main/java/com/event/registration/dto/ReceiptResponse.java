package com.event.registration.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rich receipt data used both for the API response and for generating the PDF email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptResponse {
    // Receipt identifiers
    private String receiptNo;
    private String ticketNo;

    // Event details
    private Long eventId;
    private String eventName;
    private String eventDate;
    private String venueName;
    private String venueCity;

    // Registrant details
    private Long registrantId;
    private String registrantEmail;

    // Payment details
    private BigDecimal amount;
    private String paymentId;       // Razorpay payment ID
    private String paymentMode;
    private String status;

    // Receipt metadata
    private LocalDateTime issuedAt;
}
