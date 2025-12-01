package com.DTO.ubx;

import com.models.ubx.CardDetailsEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Service
public class CardSpecs {
    public static Specification<CardDetailsEntity> createdBetween(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23,59,59);
        return (root, q, cb) -> cb.between(root.get("createdDt"), start, end);
    }

    public static Specification<CardDetailsEntity> hasStatus(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) return (r,q,c) -> c.conjunction();

        String s = status.trim().toUpperCase();
        switch (s) {
            case "F":   // Failed
            case "L":   // Linked
            case "PR":  // PIN Reissued
            case "R":   // Registered to CBS
            case "C":   // Charged
            case "PC":  // PIN Changed
            case "A":   // Activated
                return (root, q, cb) -> cb.equal(root.get("status"), s);
            default:
                return (r,q,c) -> c.conjunction();
        }
    }

    public static Specification<CardDetailsEntity> originatingBranchIs(String branch) {
        if (branch == null || "0".equals(branch) || "All".equalsIgnoreCase(branch)) return (r,q,c) -> c.conjunction();
        return (root, q, cb) -> cb.equal(root.get("originatingBranch"), branch);
    }
}