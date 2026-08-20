package com.library.booklending.loan;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findAllByOrderByIdAsc();

    long countByMemberIdAndReturnedAtIsNull(Long memberId);

    boolean existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(Long memberId, Instant dueDate);
}
