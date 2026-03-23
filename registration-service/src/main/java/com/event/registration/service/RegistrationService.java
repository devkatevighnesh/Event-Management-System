package com.event.registration.service;

import com.event.registration.client.EventServiceClient;
import com.event.registration.dto.*;
import com.event.registration.entity.*;
import com.event.registration.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

        private final RegistrationRepository registrationRepository;
        private final PaymentRepository paymentRepository;
        private final ReceiptRepository receiptRepository;
        private final EventServiceClient eventServiceClient;
        private final PdfGenerationService pdfGenerationService;
        private final EmailService emailService;

        @Value("${razorpay.key.id}")
        private String keyId;

        @Value("${razorpay.key.secret}")
        private String keySecret;

        public RegistrationService(RegistrationRepository registrationRepository,
                        PaymentRepository paymentRepository,
                        ReceiptRepository receiptRepository,
                        EventServiceClient eventServiceClient,
                        PdfGenerationService pdfGenerationService,
                        EmailService emailService) {
                this.registrationRepository = registrationRepository;
                this.paymentRepository = paymentRepository;
                this.receiptRepository = receiptRepository;
                this.eventServiceClient = eventServiceClient;
                this.pdfGenerationService = pdfGenerationService;
                this.emailService = emailService;
        }

        // ─────────────────────────────────────────────────────────────────────────
        // STEP 1: Register → creates Registration + Payment row
        // If paymentMode=RAZORPAY, also creates a Razorpay order now.
        // Returns razorpayOrderId so the frontend can open Checkout.
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional
        public RegistrationResponse register(RegistrationRequest request) throws Exception {
                ExternalEventResponse event = eventServiceClient.getEventById(request.getEventId());

                if (!"OPEN".equals(event.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not open for registration");
                }

                if (!"RAZORPAY".equalsIgnoreCase(request.getPaymentMode())
                                && !"MANUAL".equalsIgnoreCase(request.getPaymentMode())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment mode selected.");
                }

                if (registrationRepository.existsByRegistrantIdAndEventIdAndStatusNot(
                                request.getRegistrantId(), request.getEventId(), "FAILED")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "You are already registered or have a pending registration for this event.");
                }

                if (event.getMaxCapacity() != null) {
                        // Synchronous cross-service atomic ticket reservation (Optimistic Locking via
                        // Event DB)
                        try {
                                eventServiceClient.reserveTicket(request.getEventId());
                        } catch (feign.FeignException e) {
                                // Feign specific HTTP errors
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Event capacity is full or event is locked. Please try again.");
                        } catch (Exception e) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Event capacity is full. Registration closed.");
                        }
                }

                Registration registration = Registration.builder()
                                .registrantId(request.getRegistrantId())
                                .registrantEmail(request.getRegistrantEmail())
                                .eventId(request.getEventId())
                                .ticketNo("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .status("PENDING")
                                .build();
                registration = registrationRepository.save(registration);

                // Default orderId for non-Razorpay (cash/manual payments)
                String orderId = "MANUAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // Create a real Razorpay order when paymentMode is RAZORPAY
                if ("RAZORPAY".equalsIgnoreCase(request.getPaymentMode())) {
                        RazorpayClient client = new RazorpayClient(keyId, keySecret);
                        JSONObject orderRequest = new JSONObject();
                        orderRequest.put("amount", event.getEntryFee()
                                        .multiply(new java.math.BigDecimal(100)).intValue()); // paise
                        orderRequest.put("currency", "INR");
                        orderRequest.put("receipt", registration.getTicketNo());
                        Order razorpayOrder = client.orders.create(orderRequest);
                        orderId = razorpayOrder.get("id");
                }

                Payment payment = Payment.builder()
                                .registration(registration)
                                .amount(event.getEntryFee())
                                .status("PENDING")
                                .paymentMode(request.getPaymentMode())
                                .razorpayOrderId(orderId)
                                .build();
                paymentRepository.save(payment);

                return RegistrationResponse.builder()
                                .registrationId(registration.getId())
                                .eventId(registration.getEventId())
                                .ticketNo(registration.getTicketNo())
                                .status(registration.getStatus())
                                .razorpayOrderId(orderId)
                                .amount(event.getEntryFee())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // STEP 2: Return Razorpay key + order info so frontend can open Checkout.
        // The Razorpay order was already created in register(); this just
        // returns the values the frontend needs.
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional(readOnly = true)
        public PaymentOrderResponse createPaymentOrder(Long registrationId) {
                Registration registration = registrationRepository.findById(registrationId)
                                .orElseThrow(() -> new RuntimeException("Registration not found: " + registrationId));

                Payment payment = paymentRepository.findByRegistrationId(registrationId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Payment record not found for registration: " + registrationId));

                if ("SUCCESS".equals(payment.getStatus())) {
                        throw new RuntimeException("Payment already completed for this registration");
                }

                return PaymentOrderResponse.builder()
                                .registrationId(registration.getId())
                                .ticketNo(registration.getTicketNo())
                                .razorpayOrderId(payment.getRazorpayOrderId())
                                .razorpayKeyId(keyId)
                                .amount(payment.getAmount())
                                .currency("INR")
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // STEP 3 (called by Razorpay webhook): Mark payment SUCCESS,
        // Registration CONFIRMED, generate PDF receipt, send email.
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional
        public void verifyPayment(PaymentVerifyRequest request) {
                Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Payment record not found for orderId: "
                                                                + request.getRazorpayOrderId()));

                // Idempotency guard — webhook may fire more than once
                if ("SUCCESS".equals(payment.getStatus())) {
                        return;
                }

                payment.setStatus("SUCCESS");
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setPaidAt(LocalDateTime.now());

                Registration registration = payment.getRegistration();
                registration.setStatus("CONFIRMED");

                paymentRepository.save(payment);
                registrationRepository.save(registration); // was missing before — now fixed

                Receipt receipt = Receipt.builder()
                                .payment(payment)
                                .receiptNo("RCP-" + System.currentTimeMillis())
                                .build();
                receiptRepository.save(receipt);

                ExternalEventResponse event = eventServiceClient.getEventById(registration.getEventId());

                ReceiptResponse receiptResponse = ReceiptResponse.builder()
                                .receiptNo(receipt.getReceiptNo())
                                .ticketNo(registration.getTicketNo())
                                .eventId(registration.getEventId())
                                .eventName(event.getName())
                                .eventDate(event.getEventDate() != null ? event.getEventDate().toString() : null)
                                .venueName(event.getVenueName())
                                .venueCity(event.getVenueCity())
                                .registrantId(registration.getRegistrantId())
                                .registrantEmail(registration.getRegistrantEmail())
                                .amount(payment.getAmount())
                                .paymentId(payment.getRazorpayPaymentId())
                                .paymentMode(payment.getPaymentMode())
                                .status(registration.getStatus())
                                .issuedAt(receipt.getIssuedAt())
                                .build();

                String email = registration.getRegistrantEmail();
                if (email != null && !email.isBlank()) {
                        byte[] pdf = pdfGenerationService.generateReceiptPdf(receiptResponse);
                        emailService.sendReceiptEmail(
                                        email,
                                        "Your Event Registration Receipt — " + event.getName(),
                                        "Dear Registrant,\n\nThank you for registering for " + event.getName()
                                                        + ".\nPlease find your receipt attached.\n\nTicket No: "
                                                        + registration.getTicketNo(),
                                        pdf,
                                        receipt.getReceiptNo() + ".pdf");
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Approve Manual Payment (called by Admin/Organizer)
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional
        public void approveManualPayment(Long registrationId) {
                Payment payment = paymentRepository.findByRegistrationId(registrationId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Payment not found for registration: " + registrationId));

                if (!"MANUAL".equalsIgnoreCase(payment.getPaymentMode())) {
                        throw new RuntimeException("Only MANUAL payments can be approved via this endpoint.");
                }

                if ("SUCCESS".equals(payment.getStatus())) {
                        return; // Already approved
                }

                payment.setStatus("SUCCESS");
                payment.setPaidAt(LocalDateTime.now());

                Registration registration = payment.getRegistration();
                registration.setStatus("CONFIRMED");

                paymentRepository.save(payment);
                registrationRepository.save(registration);

                Receipt receipt = Receipt.builder()
                                .payment(payment)
                                .receiptNo("RCP-" + System.currentTimeMillis())
                                .build();
                receiptRepository.save(receipt);

                ExternalEventResponse event = eventServiceClient.getEventById(registration.getEventId());

                ReceiptResponse receiptResponse = ReceiptResponse.builder()
                                .receiptNo(receipt.getReceiptNo())
                                .ticketNo(registration.getTicketNo())
                                .eventId(registration.getEventId())
                                .eventName(event.getName())
                                .eventDate(event.getEventDate() != null ? event.getEventDate().toString() : null)
                                .venueName(event.getVenueName())
                                .venueCity(event.getVenueCity())
                                .registrantId(registration.getRegistrantId())
                                .registrantEmail(registration.getRegistrantEmail())
                                .amount(payment.getAmount())
                                .paymentId(payment.getRazorpayPaymentId())
                                .paymentMode(payment.getPaymentMode())
                                .status(registration.getStatus())
                                .issuedAt(receipt.getIssuedAt())
                                .build();

                String email = registration.getRegistrantEmail();
                if (email != null && !email.isBlank()) {
                        byte[] pdf = pdfGenerationService.generateReceiptPdf(receiptResponse);
                        emailService.sendReceiptEmail(
                                        email,
                                        "Your Event Registration Receipt — " + event.getName(),
                                        "Dear Registrant,\n\nYour manual payment has been approved for "
                                                        + event.getName()
                                                        + ".\nPlease find your receipt attached.\n\nTicket No: "
                                                        + registration.getTicketNo(),
                                        pdf,
                                        receipt.getReceiptNo() + ".pdf");
                }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // STEP 4: Fetch receipt by registrationId (only available after payment)
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional(readOnly = true)
        public ReceiptResponse getReceipt(Long registrationId) {
                Registration registration = registrationRepository.findById(registrationId)
                                .orElseThrow(() -> new RuntimeException("Registration not found"));

                Payment payment = paymentRepository.findByRegistrationId(registrationId)
                                .orElseThrow(() -> new RuntimeException("Payment not found"));

                Receipt receipt = receiptRepository.findByPaymentId(payment.getId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Receipt not issued yet — payment may still be pending"));

                ExternalEventResponse event = eventServiceClient.getEventById(registration.getEventId());

                return ReceiptResponse.builder()
                                .receiptNo(receipt.getReceiptNo())
                                .ticketNo(registration.getTicketNo())
                                .eventId(registration.getEventId())
                                .eventName(event.getName())
                                .eventDate(event.getEventDate() != null ? event.getEventDate().toString() : null)
                                .venueName(event.getVenueName())
                                .venueCity(event.getVenueCity())
                                .registrantId(registration.getRegistrantId())
                                .registrantEmail(registration.getRegistrantEmail())
                                .amount(payment.getAmount())
                                .paymentId(payment.getRazorpayPaymentId())
                                .paymentMode(payment.getPaymentMode())
                                .status(registration.getStatus())
                                .issuedAt(receipt.getIssuedAt())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Get all registrations for a user (history + pending)
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional(readOnly = true)
        public java.util.List<RegistrationResponse> getMyRegistrations(Long registrantId) {
                return registrationRepository.findByRegistrantId(registrantId).stream().map(reg -> {
                        String orderId = null;
                        java.math.BigDecimal amount = java.math.BigDecimal.ZERO;

                        java.util.Optional<Payment> paymentOpt = paymentRepository.findByRegistrationId(reg.getId());
                        if (paymentOpt.isPresent()) {
                                orderId = paymentOpt.get().getRazorpayOrderId();
                                amount = paymentOpt.get().getAmount();
                        }

                        return RegistrationResponse.builder()
                                        .registrationId(reg.getId())
                                        .eventId(reg.getEventId())
                                        .ticketNo(reg.getTicketNo())
                                        .status(reg.getStatus())
                                        .razorpayOrderId(orderId)
                                        .amount(amount)
                                        .build();
                }).collect(java.util.stream.Collectors.toList());
        }

        // ─────────────────────────────────────────────────────────────────────────
        // STEP 5: Mark Payment Failed (called by Razorpay webhook or manual timeout)
        // Frees up the ticket capacity by marking Registration as FAILED.
        // ─────────────────────────────────────────────────────────────────────────
        @Transactional
        public void markPaymentFailed(String razorpayOrderId) {
                paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        if ("PENDING".equals(payment.getStatus())) {
                                payment.setStatus("FAILED");
                                payment.setPaidAt(LocalDateTime.now()); // time of failure
                                paymentRepository.save(payment);

                                Registration registration = payment.getRegistration();
                                if (registration != null) {
                                        registration.setStatus("FAILED");
                                        registrationRepository.save(registration);

                                        // Release the capacity back to the Event Service
                                        try {
                                                eventServiceClient.releaseTicket(registration.getEventId());
                                        } catch (Exception e) {
                                                System.err.println("Failed to release ticket for event "
                                                                + registration.getEventId());
                                        }
                                }
                        }
                });
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Run every minute to clean up expired PENDING registrations (10 minutes)
        // ─────────────────────────────────────────────────────────────────────────
        @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
        @Transactional
        public void cancelExpiredRegistrations() {
                LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
                java.util.List<Registration> expiredRegistrations = registrationRepository
                                .findByStatusAndRegisteredAtBefore("PENDING", cutoff);

                for (Registration registration : expiredRegistrations) {
                        try {
                                cancelRegistrationInternal(registration);
                        } catch (Exception e) {
                                System.err.println("Error cancelling expired registration: " + registration.getId());
                                e.printStackTrace();
                        }
                }
        }

        @Transactional
        public void cancelRegistrationInternal(Registration registration) {
                if (!"PENDING".equals(registration.getStatus())) {
                        return;
                }

                registration.setStatus("FAILED");
                registrationRepository.save(registration);

                paymentRepository.findByRegistrationId(registration.getId()).ifPresent(payment -> {
                        if ("PENDING".equals(payment.getStatus())) {
                                payment.setStatus("FAILED");
                                payment.setPaidAt(LocalDateTime.now());
                                paymentRepository.save(payment);
                        }
                });

                // Release the capacity back to the Event Service
                try {
                        eventServiceClient.releaseTicket(registration.getEventId());
                        System.out.println("Released ticket for expired registration: " + registration.getId());
                } catch (Exception e) {
                        System.err.println("Failed to release ticket for event " + registration.getEventId());
                }
        }
}
