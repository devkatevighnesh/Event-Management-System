package com.event.service.controller;

import com.event.service.dto.EventRequest;
import com.event.service.dto.EventResponse;
import com.event.service.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    @org.springframework.beans.factory.annotation.Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    // @GetMapping
    // public ResponseEntity<List<EventResponse>> getEvents(
    // @RequestParam(required = false) String name,
    // @RequestParam(required = false) String venue,
    // @RequestParam(required = false) BigDecimal minFee,
    // @RequestParam(required = false) BigDecimal maxFee,
    // @RequestParam(required = false, name = "start") @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
    // @RequestParam(required = false, name = "end") @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    // return ResponseEntity.ok(eventService.getAllEvents(name, venue, minFee,
    // maxFee, start, end));
    // }
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) BigDecimal minFee,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false, name = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false, name = "end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(
                eventService.getAllEvents(name, venue, minFee, maxFee, start, end));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        eventService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserveTicket(@PathVariable Long id) {
        eventService.reserveTicket(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseTicket(@PathVariable Long id) {
        eventService.releaseTicket(id);
        return ResponseEntity.ok().build();
    }
}
