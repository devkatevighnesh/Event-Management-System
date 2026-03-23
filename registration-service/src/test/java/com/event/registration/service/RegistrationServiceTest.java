package com.event.registration.service;

import com.event.registration.client.EventServiceClient;
import com.event.registration.dto.*;
import com.event.registration.entity.Payment;
import com.event.registration.entity.Registration;
import com.event.registration.repository.PaymentRepository;
import com.event.registration.repository.ReceiptRepository;
import com.event.registration.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private EventServiceClient eventServiceClient;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistrationService registrationService;

    private ExternalEventResponse mockEvent;

    @BeforeEach
    public void setup() {
        mockEvent = ExternalEventResponse.builder()
                .id(10L)
                .name("Test Event")
                .status("OPEN")
                .entryFee(new BigDecimal("100.00"))
                .maxCapacity(50)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 1. register_Success (Using MANUAL payment for simplicity)
    // ─────────────────────────────────────────────────────────────
    @Test
    public void register_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setEventId(10L);
        request.setRegistrantId(1L);
        request.setPaymentMode("MANUAL");

        when(eventServiceClient.getEventById(10L)).thenReturn(mockEvent);
        when(registrationRepository.existsByRegistrantIdAndEventIdAndStatusNot(1L, 10L, "FAILED")).thenReturn(false);
        when(registrationRepository.countByEventIdAndStatusIn(eq(10L), anyList())).thenReturn(10L); // 10/50 capacity
        
        Registration mockRegistration = Registration.builder().id(99L).ticketNo("TKT-123").status("PENDING").build();
        when(registrationRepository.save(any(Registration.class))).thenReturn(mockRegistration);

        RegistrationResponse response = registrationService.register(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(registrationRepository).save(any(Registration.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. register_EventClosed_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void register_EventClosed_ThrowsException() {
        mockEvent.setStatus("CLOSED");
        RegistrationRequest request = new RegistrationRequest();
        request.setEventId(10L);

        when(eventServiceClient.getEventById(10L)).thenReturn(mockEvent);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            registrationService.register(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Event is not open"));
    }

    // ─────────────────────────────────────────────────────────────
    // 3. register_EventFull_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void register_EventFull_ThrowsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEventId(10L);
        request.setPaymentMode("MANUAL");

        when(eventServiceClient.getEventById(10L)).thenReturn(mockEvent);
        when(registrationRepository.countByEventIdAndStatusIn(eq(10L), anyList())).thenReturn(50L); // 50/50 full!

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            registrationService.register(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Event capacity is full"));
    }

    // ─────────────────────────────────────────────────────────────
    // 4. register_DuplicateRegistration_ThrowsException
    // ─────────────────────────────────────────────────────────────
    @Test
    public void register_DuplicateRegistration_ThrowsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEventId(10L);
        request.setRegistrantId(5L);
        request.setPaymentMode("MANUAL");

        when(eventServiceClient.getEventById(10L)).thenReturn(mockEvent);
        // Mock that user 5 is already registered
        when(registrationRepository.existsByRegistrantIdAndEventIdAndStatusNot(5L, 10L, "FAILED")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            registrationService.register(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already registered"));
    }

    // ─────────────────────────────────────────────────────────────
    // 5. markPaymentFailed_Success
    // ─────────────────────────────────────────────────────────────
    @Test
    public void markPaymentFailed_Success() {
        Registration registration = Registration.builder().id(20L).status("PENDING").build();
        Payment payment = Payment.builder().id(30L).status("PENDING").razorpayOrderId("order_ABC").registration(registration).build();

        when(paymentRepository.findByRazorpayOrderId("order_ABC")).thenReturn(Optional.of(payment));

        registrationService.markPaymentFailed("order_ABC");

        assertEquals("FAILED", payment.getStatus());
        assertEquals("FAILED", registration.getStatus());
        verify(paymentRepository).save(payment);
        verify(registrationRepository).save(registration);
    }
}
