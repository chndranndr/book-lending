package com.library.booklending.loan;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class LoanDtos {

    private LoanDtos() {
    }

    public record BorrowLoanRequest(
            @NotNull Long bookId,
            @NotNull Long memberId
    ) {
    }

    public record LoanResponse(
            Long id,
            Long bookId,
            Long memberId,
            Instant borrowedAt,
            Instant dueDate,
            Instant returnedAt
    ) {
    }
}
