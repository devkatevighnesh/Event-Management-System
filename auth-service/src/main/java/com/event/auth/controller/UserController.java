package com.event.auth.controller;

import com.event.auth.dto.ProfileRequest;
import com.event.auth.dto.ProfileResponse;
import com.event.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/user")
public class UserController {

    @org.springframework.beans.factory.annotation.Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody ProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
}
