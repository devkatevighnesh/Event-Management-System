package com.event.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long registrantId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String registrantEmail;

    @Column(nullable = false, unique = true)
    private String ticketNo;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, CANCELLED

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
