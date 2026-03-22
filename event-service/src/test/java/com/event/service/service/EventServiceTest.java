package com.event.service.service;

import com.event.service.dto.EventRequest;
import com.event.service.dto.EventResponse;
import com.event.service.entity.Event;
import com.event.service.entity.Venue;
import com.event.service.repository.EventRepository;
import com.event.service.repository.VenueRepository;
import com.event.service.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventService eventService;

    private UserPrincipal mockPrincipal;
    private Venue mockVenue;
    private Event mockEvent;

    @BeforeEach
    public void setup() {
        // Setup mock user in Spring Security Context
        mockPrincipal = new UserPrincipal(10L, "org_user", "ROLE_ORGANIZER");
        SecurityContextHolder.setContext(securityContext);

        mockVenue = Venue.builder().id(1L).name("Expo Center").city("New York").build();
        mockEvent = Event.builder()
                .id(100L)
                .name("Tech Meetup")
                .venue(mockVenue)
                .organizerId(10L)
                .status("OPEN")
                .maxCapacity(50)
                .build();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. createEvent_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void createEvent_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);

        EventRequest request = new EventRequest();
        request.setName("New Event");
        request.setVenueId(1L);
        request.setMaxCapacity(100);

        when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
        
        Event savedEvent = Event.builder().id(200L).name("New Event").venue(mockVenue).organizerId(10L).status("CLOSED").maxCapacity(100).build();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(request);

        assertNotNull(response);
        assertEquals("New Event", response.getName());
        assertEquals("Expo Center", response.getVenueName());
        verify(eventRepository).save(any(Event.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. createEvent_VenueNotFound_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void createEvent_VenueNotFound_ThrowsException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);

        EventRequest request = new EventRequest();
        request.setVenueId(99L); // unknown venue

        when(venueRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.createEvent(request);
        });

        assertEquals("Venue not found", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. updateEvent_Success (Owner logs in)
    // ─────────────────────────────────────────────────────────────
    @Test
    public void updateEvent_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);

        EventRequest request = new EventRequest();
        request.setName("Updated Event Name");

        when(eventRepository.findById(100L)).thenReturn(Optional.of(mockEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        EventResponse response = eventService.updateEvent(100L, request);

        assertNotNull(response);
        verify(eventRepository).save(mockEvent);
        assertEquals("Updated Event Name", mockEvent.getName()); // checks if modification was applied
    }

    // ─────────────────────────────────────────────────────────────
    // 4. updateEvent_NotAuthorized_ThrowsException (Wrong Organizer)
    // ─────────────────────────────────────────────────────────────
    @Test
    public void updateEvent_NotAuthorized_ThrowsException() {
        // Logged in as user ID 999
        UserPrincipal hackerPrincipal = new UserPrincipal(999L, "hacker", "ROLE_ORGANIZER");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(hackerPrincipal);

        // Event is owned by user ID 10
        when(eventRepository.findById(100L)).thenReturn(Optional.of(mockEvent));

        EventRequest request = new EventRequest();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.updateEvent(100L, request);
        });

        assertEquals("You are not authorized to manage this event", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. deleteEvent_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void deleteEvent_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(mockEvent));

        eventService.deleteEvent(100L);

        verify(eventRepository).delete(mockEvent);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. getEventById_NotFound_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void getEventById_NotFound_ThrowsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.getEventById(999L);
        });

        assertEquals("Event not found", exception.getMessage());
    }
}
