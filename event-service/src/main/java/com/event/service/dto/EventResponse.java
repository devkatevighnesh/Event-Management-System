package com.event.service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String venueName;
    private String venueCity;
    private LocalDateTime eventDate;
    private BigDecimal entryFee;
    private String status;
    private Integer maxCapacity;
    private Integer availableTickets;
    private String organizerName; // Fetched async
}
