package com.library.booklending.loan;

import com.library.booklending.book.BookRepository;
import com.library.booklending.book.BookService;
import com.library.booklending.common.BusinessErrorCode;
import com.library.booklending.common.BusinessException;
import com.library.booklending.common.LendingEventLogger;
import com.library.booklending.config.BorrowingProperties;
import com.library.booklending.member.MemberService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final MemberService memberService;
    private final BorrowingProperties borrowingProperties;
    private final Clock clock;
    private final LendingEventLogger eventLogger;

    public LoanService(
            LoanRepository loanRepository,
            BookRepository bookRepository,
            BookService bookService,
            MemberService memberService,
            BorrowingProperties borrowingProperties,
            Clock clock,
            LendingEventLogger eventLogger
    ) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.memberService = memberService;
        this.borrowingProperties = borrowingProperties;
        this.clock = clock;
        this.eventLogger = eventLogger;
    }

    @Transactional(readOnly = true)
    public List<LoanDtos.LoanResponse> findAll() {
        return loanRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoanDtos.LoanResponse findById(Long id) {
        return toResponse(getLoan(id));
    }

    @Transactional
    public LoanDtos.LoanResponse borrowBook(LoanDtos.BorrowLoanRequest request) {
        memberService.getMember(request.memberId());

        long activeLoanCount = loanRepository.countByMemberIdAndReturnedAtIsNull(request.memberId());
        if (activeLoanCount >= borrowingProperties.maxActiveLoans()) {
            eventLogger.loanRejected(request.memberId(), request.bookId(), BusinessErrorCode.MAX_ACTIVE_LOANS_EXCEEDED);
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Borrowing rejected",
                    "Member has reached the maximum number of active loans",
                    BusinessErrorCode.MAX_ACTIVE_LOANS_EXCEEDED
            );
        }

        Instant now = Instant.now(clock);
        if (loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(request.memberId(), now)) {
            eventLogger.loanRejected(request.memberId(), request.bookId(), BusinessErrorCode.OVERDUE_LOAN);
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Borrowing rejected",
                    "Member has an overdue loan",
                    BusinessErrorCode.OVERDUE_LOAN
            );
        }

        bookService.getBook(request.bookId());
        int updatedRows = bookRepository.decrementAvailableCopiesIfAvailable(request.bookId());
        if (updatedRows == 0) {
            eventLogger.loanRejected(request.memberId(), request.bookId(), BusinessErrorCode.BOOK_UNAVAILABLE);
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Borrowing rejected",
                    "Book has no available copies",
                    BusinessErrorCode.BOOK_UNAVAILABLE
            );
        }

        Instant dueDate = now.plus(borrowingProperties.loanDurationDays(), ChronoUnit.DAYS);
        Loan loan = loanRepository.save(new Loan(request.bookId(), request.memberId(), now, dueDate));
        eventLogger.loanBorrowed(request.memberId(), request.bookId(), loan.getId());
        return toResponse(loan);
    }

    @Transactional
    public LoanDtos.LoanResponse returnBook(Long id) {
        Loan loan = getLoan(id);
        if (loan.isReturned()) {
            return toResponse(loan);
        }

        Instant now = Instant.now(clock);
        loan.markReturned(now);
        bookRepository.incrementAvailableCopies(loan.getBookId());
        Loan savedLoan = loanRepository.save(loan);
        eventLogger.loanReturned(savedLoan.getId(), savedLoan.getBookId());
        return toResponse(savedLoan);
    }

    @Transactional(readOnly = true)
    public Loan getLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Loan not found",
                        "Loan " + id + " does not exist",
                        BusinessErrorCode.LOAN_NOT_FOUND
                ));
    }

    private LoanDtos.LoanResponse toResponse(Loan loan) {
        return new LoanDtos.LoanResponse(
                loan.getId(),
                loan.getBookId(),
                loan.getMemberId(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt()
        );
    }
}
