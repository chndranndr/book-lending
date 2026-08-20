package com.library.booklending.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookRequest(
            @NotBlank String title,
            @NotBlank String author,
            @NotBlank String isbn,
            @Min(0) int totalCopies
    ) {
    }

    public record BookResponse(
            Long id,
            String title,
            String author,
            String isbn,
            int totalCopies,
            int availableCopies
    ) {
    }
}
