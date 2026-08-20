package com.library.booklending.loan;

import lombok.Getter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "loan")
@Getter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected Loan() {
    }

    public Loan(Long bookId, Long memberId, Instant borrowedAt, Instant dueDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.borrowedAt = borrowedAt;
        this.dueDate = dueDate;
    }

    public boolean isReturned() {
        return returnedAt != null;
    }

    public void markReturned(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }
}
