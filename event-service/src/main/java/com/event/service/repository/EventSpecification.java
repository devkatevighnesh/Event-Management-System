package com.event.service.repository;

import com.event.service.entity.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> filter(
            String name,
            String venueName,
            BigDecimal minFee,
            BigDecimal maxFee,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only add each predicate if the parameter is NOT null
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (venueName != null && !venueName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.join("venue").get("name")), "%" + venueName.toLowerCase() + "%"));
            }
            if (minFee != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryFee"), minFee));
            }
            if (maxFee != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryFee"), maxFee));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
