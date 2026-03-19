package com.event.registration.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptResponse {
    private String receiptNo;
    private String ticketNo;
    private Long eventId;
    private String eventName;
    private Long registrantId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime issuedAt;
}
