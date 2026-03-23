package com.event.registration.controller;

import com.event.registration.dto.*;
import com.event.registration.security.JwtUtil;
import com.event.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Registration API
 * ──────────────────────────────────────────────
 * STEP 1:  POST /registration/register
 *          → Creates Registration (status: PENDING)
 *          → Returns: registrationId, ticketNo
 *
 * STEP 2:  POST /registration/payment/order/{registrationId}
 *          → Creates a Razorpay Order for the registration
 *          → Returns: razorpayOrderId, razorpayKeyId, amount
 *          → Frontend opens Razorpay Checkout modal with these values
 *
 *          [User pays inside Razorpay UI]
 *
 * STEP 3:  (Automatic) Razorpay calls POST /registration/webhooks/razorpay
 *          → HMAC signature verified
 *          → Payment marked SUCCESS, Registration marked CONFIRMED
 *          → PDF receipt generated and emailed to registrant
 *
 * STEP 4:  GET /registration/receipt/{registrationId}
 *          → Fetch full receipt details
 */
@RestController
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JwtUtil jwtUtil;

    // STEP 1: Initiate registration
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RegistrationRequest request) throws Exception {

        // Extract email from JWT and attach to request
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractClaim(token, claims -> claims.get("email", String.class));
            request.setRegistrantEmail(email);
        }
        return ResponseEntity.ok(registrationService.register(request));
    }

    // STEP 2: Create Razorpay order for a registration
    @PostMapping("/payment/order/{registrationId}")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
            @PathVariable Long registrationId) throws Exception {
        return ResponseEntity.ok(registrationService.createPaymentOrder(registrationId));
    }

    // STEP 4: Fetch receipt (only available after successful payment)
    @GetMapping("/receipt/{registrationId}")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable Long registrationId) {
        return ResponseEntity.ok(registrationService.getReceipt(registrationId));
    }

    // STEP 5: Approve manual payment (Admin / Organizer only)
    @PostMapping("/admin/payment/approve/{registrationId}")
    public ResponseEntity<String> approveManualPayment(@PathVariable Long registrationId) {
        registrationService.approveManualPayment(registrationId);
        return ResponseEntity.ok("Manual payment approved successfully");
    }

    // Get all registrations for the logged in user
    @GetMapping("/my-registrations")
    public ResponseEntity<java.util.List<RegistrationResponse>> getMyRegistrations(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Integer userIdInt = jwtUtil.extractClaim(token, claims -> claims.get("userId", Integer.class));
            Long userId = userIdInt != null ? userIdInt.longValue() : null;
            
            if (userId != null) {
                return ResponseEntity.ok(registrationService.getMyRegistrations(userId));
            }
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
    }
}
