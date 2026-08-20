package com.library.booklending;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.booklending.book.BookDtos;
import com.library.booklending.loan.LoanDtos;
import com.library.booklending.member.MemberDtos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OverdueLoanTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-15T10:00:00Z");

    @TestConfiguration
    static class Config {
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void borrowRejectedWhenMemberHasOverdueLoan() throws Exception {
        long bookId = createBook("Book", "Author", "ISBN-OVERDUE", 5);
        long memberId = createMember("Overdue User", "overdue@test.com");

        // Insert a loan with due_date in the past relative to FIXED_NOW
        jdbcTemplate.update(
                "INSERT INTO loan (book_id, member_id, borrowed_at, due_date, returned_at) VALUES (?, ?, ?, ?, ?)",
                bookId, memberId,
                "2026-05-01T10:00:00Z",
                "2026-06-10T10:00:00Z", // before FIXED_NOW -> overdue
                null
        );

        // Decrement available copies to keep inventory consistent
        jdbcTemplate.update("UPDATE book SET available_copies = available_copies - 1 WHERE id = ?", bookId);

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanDtos.BorrowLoanRequest(bookId, memberId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVERDUE_LOAN"));
    }

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
}
