package com.event.registration.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalEventResponse {
    private Long id;
    private String name;
    private String status;
    private Integer maxCapacity;
    private BigDecimal entryFee;
    private java.time.LocalDateTime eventDate;
    private String venueName;
    private String venueCity;
}
