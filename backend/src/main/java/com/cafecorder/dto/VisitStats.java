package com.cafecorder.dto;

import java.math.BigDecimal;

// averageRating is null when nothing has been rated yet
public record VisitStats(long visits, long uniqueCafes, BigDecimal totalSpent, Double averageRating) {
}
