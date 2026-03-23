package com.event.registration.repository;

import com.event.registration.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByTicketNo(String ticketNo);

    // For Duplicate Prevention: True if user has any registration that is NOT "FAILED"
    boolean existsByRegistrantIdAndEventIdAndStatusNot(Long registrantId, Long eventId, String status);

    // For Capacity Enforcement: Count registrations that are "PENDING" or "CONFIRMED"
    long countByEventIdAndStatusIn(Long eventId, java.util.List<String> statuses);

    // Get all registrations for a specific user
    java.util.List<Registration> findByRegistrantId(Long registrantId);

    // Get all registrations by status before a specific time
    java.util.List<Registration> findByStatusAndRegisteredAtBefore(String status, java.time.LocalDateTime cutoffTime);
}
