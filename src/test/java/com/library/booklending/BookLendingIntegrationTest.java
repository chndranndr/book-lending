package com.library.booklending;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.booklending.book.BookDtos;
import com.library.booklending.loan.LoanDtos;
import com.library.booklending.member.MemberDtos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookLendingIntegrationTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-01T10:00:00Z");

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Borrowing tests ---

    @Test
    void borrowSucceeds() throws Exception {
        long bookId = createBook("Test Book", "Author", "ISBN-001", 3);
        long memberId = createMember("Alice", "alice@example.com");

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.borrowedAt").value("2026-06-01T10:00:00Z"))
                .andExpect(jsonPath("$.returnedAt").isEmpty());
    }

    @Test
    void dueDateUsesConfiguredDuration() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-002", 1);
        long memberId = createMember("Bob", "bob@example.com");

        Instant expectedDue = FIXED_NOW.plus(14, ChronoUnit.DAYS);

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value(expectedDue.toString()));
    }

    @Test
    void borrowRejectedWhenMaxActiveLoansReached() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-003", 10);
        long memberId = createMember("Charlie", "charlie@example.com");

        for (int i = 0; i < 3; i++) {
            borrowBook(bookId, memberId);
        }

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_ACTIVE_LOANS_EXCEEDED"));
    }

    // Overdue loan rejection is tested in OverdueLoanTest which uses JdbcTemplate for setup

    @Test
    void borrowRejectedWhenNoCopiesAvailable() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-005", 1);
        long member1 = createMember("Eve", "eve@example.com");
        long member2 = createMember("Frank", "frank@example.com");

        borrowBook(bookId, member1);

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, member2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_UNAVAILABLE"));
    }

    @Test
    void borrowDecreasesAvailableCopies() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-006", 3);
        long memberId = createMember("Grace", "grace@example.com");

        borrowBook(bookId, memberId);

        mockMvc.perform(get("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(2));
    }

    @Test
    void returnSetsReturnedAt() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-007", 1);
        long memberId = createMember("Heidi", "heidi@example.com");
        long loanId = borrowBook(bookId, memberId);

        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").value("2026-06-01T10:00:00Z"));
    }

    @Test
    void returnIncreasesAvailableCopies() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-008", 1);
        long memberId = createMember("Ivan", "ivan@example.com");
        long loanId = borrowBook(bookId, memberId);

        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void doubleReturnDoesNotIncrementTwice() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-009", 1);
        long memberId = createMember("Judy", "judy@example.com");
        long loanId = borrowBook(bookId, memberId);

        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void bookUpdateCannotSetTotalCopiesBelowBorrowed() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-010", 3);
        long memberId = createMember("Karl", "karl@example.com");
        borrowBook(bookId, memberId);
        borrowBook(bookId, memberId);

        mockMvc.perform(put("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest("Book", "Author", "ISBN-010", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TOTAL_COPIES"));
    }

    @Test
    void bookCrudSupportsReadUpdateAndDelete() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-CRUD", 2);

        mockMvc.perform(get("/api/books").with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookId));

        mockMvc.perform(get("/api/books/" + bookId).with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Book"))
                .andExpect(jsonPath("$.availableCopies").value(2));

        mockMvc.perform(put("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BookDtos.BookRequest("Updated Book", "Updated Author", "ISBN-CRUD-UPDATED", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Book"))
                .andExpect(jsonPath("$.availableCopies").value(3));

        mockMvc.perform(delete("/api/books/" + bookId).with(httpBasic("admin", "admin-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + bookId).with(httpBasic("admin", "admin-password")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void memberCrudSupportsReadUpdateAndDelete() throws Exception {
        long memberId = createMember("Member", "member-crud@example.com");

        mockMvc.perform(get("/api/members").with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(memberId));

        mockMvc.perform(get("/api/members/" + memberId).with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member-crud@example.com"));

        mockMvc.perform(put("/api/members/" + memberId)
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MemberDtos.MemberRequest("Updated Member", "updated-member@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Member"));

        mockMvc.perform(delete("/api/members/" + memberId).with(httpBasic("admin", "admin-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/" + memberId).with(httpBasic("admin", "admin-password")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void loanCanBeListedAndReadById() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-LOAN-READ", 1);
        long memberId = createMember("Member", "loan-read@example.com");
        long loanId = borrowBook(bookId, memberId);

        mockMvc.perform(get("/api/loans").with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(loanId));

        mockMvc.perform(get("/api/loans/" + loanId).with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(bookId))
                .andExpect(jsonPath("$.memberId").value(memberId));
    }

    // --- Persistence tests ---

    @Test
    void isbnUniquenessEnforced() throws Exception {
        createBook("Book 1", "Author", "UNIQUE-ISBN", 1);

        mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest("Book 2", "Author", "UNIQUE-ISBN", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ISBN_ALREADY_EXISTS"));
    }

    @Test
    void emailUniquenessEnforced() throws Exception {
        createMember("User 1", "same@example.com");

        mockMvc.perform(post("/api/members")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberDtos.MemberRequest("User 2", "same@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loanReferencesRealBookAndMember() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(9999L, 9999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBookBlockedByLoanReference() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-FK", 1);
        long memberId = createMember("Member", "fk@example.com");
        borrowBook(bookId, memberId);

        mockMvc.perform(delete("/api/books/" + bookId)
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void deleteMemberBlockedByLoanReference() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-MEMBER-FK", 1);
        long memberId = createMember("Member", "member-fk@example.com");
        borrowBook(bookId, memberId);

        mockMvc.perform(delete("/api/members/" + memberId)
                        .with(httpBasic("admin", "admin-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void invalidRequestsReturnValidationProblemDetails() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest("", "", "", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/members")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberDtos.MemberRequest("", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void healthMetricsAndOpenApiAreAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/metrics").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    // --- Security tests ---

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void librarianCannotMutateBooks() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("librarian", "librarian-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest("Book", "Author", "ISBN-SEC", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateBook() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest("Book", "Author", "ISBN-ADM", 1))))
                .andExpect(status().isCreated());
    }

    @Test
    void librarianCanBorrowAndReturn() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-LIB", 1);
        long memberId = createMember("Lib User", "lib@example.com");

        MvcResult result = mockMvc.perform(post("/api/loans")
                        .with(httpBasic("librarian", "librarian-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isCreated())
                .andReturn();

        long loanId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/loans/" + loanId + "/return")
                        .with(httpBasic("librarian", "librarian-password")))
                .andExpect(status().isOk());
    }


    // --- Helpers ---

    private long createBook(String title, String author, String isbn, int copies) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookDtos.BookRequest(title, author, isbn, copies))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createMember(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberDtos.MemberRequest(name, email))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long borrowBook(long bookId, long memberId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
