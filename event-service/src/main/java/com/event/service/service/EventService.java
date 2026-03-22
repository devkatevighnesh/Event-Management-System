package com.event.service.service;

import com.event.service.dto.EventRequest;
import com.event.service.dto.EventResponse;
import com.event.service.entity.Event;
import com.event.service.entity.Venue;
import com.event.service.repository.EventRepository;
import com.event.service.repository.EventSpecification;
import com.event.service.repository.VenueRepository;
import com.event.service.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long organizerId = principal.getUserId();

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(venue)
                .eventDate(request.getEventDate())
                .entryFee(request.getEntryFee())
                .maxCapacity(request.getMaxCapacity())
                .availableTickets(request.getMaxCapacity())
                .organizerId(organizerId)
                .organizerName(principal.getUsername())
                .status("CLOSED") // Default status
                .build();

        event = eventRepository.save(event);
        return mapToResponse(event);
    }

    // @Transactional(readOnly = true)
    // public List<EventResponse> getAllEvents(String name, String venue, BigDecimal
    // minFee, BigDecimal maxFee,
    // LocalDateTime start, LocalDateTime end) {
    // String namePattern = (name != null) ? "%" + name.toLowerCase() + "%" : null;
    // String venuePattern = (venue != null) ? "%" + venue.toLowerCase() + "%" :
    // null;
    // return eventRepository.filterEvents(namePattern, venuePattern, minFee,
    // maxFee, start, end)
    // .stream()
    // .map(this::mapToResponse)
    // .collect(Collectors.toList());
    // }
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(String name,
            String venue,
            BigDecimal minFee,
            BigDecimal maxFee,
            LocalDateTime start,
            LocalDateTime end) {

        return eventRepository.findAll(EventSpecification.filter(name, venue, minFee, maxFee, start, end))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return mapToResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        checkOwnership(event);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found"));
            event.setVenue(venue);
        }

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setEntryFee(request.getEntryFee());

        if (request.getMaxCapacity() != null && !request.getMaxCapacity().equals(event.getMaxCapacity())) {
            int difference = request.getMaxCapacity() - event.getMaxCapacity();
            event.setMaxCapacity(request.getMaxCapacity());
            event.setAvailableTickets(event.getAvailableTickets() + difference);
        }

        event = eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        checkOwnership(event);
        eventRepository.delete(event);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        checkOwnership(event);
        event.setStatus(status);
        eventRepository.save(event);
    }

    @Transactional
    public void reserveTicket(Long id) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
        if (!"OPEN".equals(event.getStatus()))
            throw new RuntimeException("Event is not open for registration");

        // Lazy initialization for legacy events
        if (event.getAvailableTickets() == null) {
            event.setAvailableTickets(event.getMaxCapacity());
        }

        if (event.getAvailableTickets() <= 0)
            throw new RuntimeException("Event capacity is full");

        event.setAvailableTickets(event.getAvailableTickets() - 1);
        eventRepository.save(event);
    }

    @Transactional
    public void releaseTicket(Long id) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));

        // Lazy initialization for legacy events
        if (event.getAvailableTickets() == null) {
            event.setAvailableTickets(event.getMaxCapacity());
        }

        event.setAvailableTickets(event.getAvailableTickets() + 1);
        eventRepository.save(event);
    }

    private void checkOwnership(Event event) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!event.getOrganizerId().equals(principal.getUserId()) && !"ADMIN".equals(principal.getRole())) {
            throw new RuntimeException("You are not authorized to manage this event");
        }
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venueName(event.getVenue().getName())
                .venueCity(event.getVenue().getCity())
                .eventDate(event.getEventDate())
                .entryFee(event.getEntryFee())
                .status(event.getStatus())
                .maxCapacity(event.getMaxCapacity())
                .availableTickets(
                        event.getAvailableTickets() == null ? event.getMaxCapacity() : event.getAvailableTickets())
                .organizerName(event.getOrganizerName())
                .build();
    }
}
