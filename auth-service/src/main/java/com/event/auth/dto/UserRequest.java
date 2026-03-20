package com.event.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    private String username;
    private String email;
    private String password;
    private String role;
    
    // Profile Fields
    private String fullName;
    private String phone;
    private java.time.LocalDate dateOfBirth;
    private String bio;
}
