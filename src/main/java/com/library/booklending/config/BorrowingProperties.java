package com.library.booklending.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.borrowing")
public record BorrowingProperties(
        @Positive int maxActiveLoans,
        @Positive int loanDurationDays
) {
}
