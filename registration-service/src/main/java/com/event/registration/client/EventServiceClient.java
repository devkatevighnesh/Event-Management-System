package com.event.registration.client;

import com.event.registration.dto.ExternalEventResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    @GetMapping("/events/{id}")
    ExternalEventResponse getEventById(@PathVariable("id") Long id);

    @PostMapping("/events/{id}/reserve")
    void reserveTicket(@PathVariable("id") Long id);

    @PostMapping("/events/{id}/release")
    void releaseTicket(@PathVariable("id") Long id);
}
