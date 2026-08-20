package com.library.booklending.loan;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanDtos.LoanResponse> borrowBook(@Valid @RequestBody LoanDtos.BorrowLoanRequest request) {
        LoanDtos.LoanResponse response = loanService.borrowBook(request);
        return ResponseEntity.created(URI.create("/api/loans/" + response.id())).body(response);
    }

    @GetMapping
    public List<LoanDtos.LoanResponse> findAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public LoanDtos.LoanResponse findById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @PostMapping("/{id}/return")
    public LoanDtos.LoanResponse returnBook(@PathVariable Long id) {
        return loanService.returnBook(id);
    }
}
