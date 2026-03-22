package com.event.service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {
    private String name;
    private String description;
    private Long venueId;
    private LocalDateTime eventDate;
    private BigDecimal entryFee;
    private Integer maxCapacity;
}
