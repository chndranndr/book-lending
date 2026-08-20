package com.library.booklending.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class MemberDtos {

    private MemberDtos() {
    }

    public record MemberRequest(
            @NotBlank String name,
            @NotBlank @Email String email
    ) {
    }

    public record MemberResponse(
            Long id,
            String name,
            String email
    ) {
    }
}
