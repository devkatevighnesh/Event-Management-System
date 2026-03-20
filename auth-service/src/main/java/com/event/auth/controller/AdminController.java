package com.event.auth.controller;

import com.event.auth.dto.UserRequest;
import com.event.auth.dto.UserResponse;
import com.event.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @org.springframework.beans.factory.annotation.Autowired
    private AuthService authService;

    @PostMapping("/organizers")
    public ResponseEntity<UserResponse> addOrganizer(@RequestBody UserRequest request) {
        request.setRole("ORGANIZER");
        return ResponseEntity.ok(authService.createUser(request));
    }

    @GetMapping("/organizers")
    public ResponseEntity<java.util.List<UserResponse>> getOrganizers() {
        return ResponseEntity.ok(authService.getUsersByRole("ORGANIZER"));
    }

    @PostMapping("/registrants")
    public ResponseEntity<UserResponse> addRegistrant(@RequestBody UserRequest request) {
        request.setRole("REGISTRANT");
        return ResponseEntity.ok(authService.createUser(request));
    }

    @GetMapping("/registrants")
    public ResponseEntity<java.util.List<UserResponse>> getRegistrants() {
        return ResponseEntity.ok(authService.getUsersByRole("REGISTRANT"));
    }
}
