package com.event.auth.controller;

import com.event.auth.dto.LoginRequest;
import com.event.auth.dto.LoginResponse;
import com.event.auth.dto.UserRequest;
import com.event.auth.dto.UserResponse;
import com.event.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @org.springframework.beans.factory.annotation.Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/organizer")
    public ResponseEntity<UserResponse> registerOrganizer(@RequestBody UserRequest request) {
        request.setRole("ORGANIZER");
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PostMapping("/register/registrant")
    public ResponseEntity<UserResponse> registerRegistrant(@RequestBody UserRequest request) {
        request.setRole("REGISTRANT");
        return ResponseEntity.ok(authService.createUser(request));
    }
}
