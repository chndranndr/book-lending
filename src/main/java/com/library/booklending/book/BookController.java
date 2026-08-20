package com.library.booklending.book;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookDtos.BookResponse> create(@Valid @RequestBody BookDtos.BookRequest request) {
        BookDtos.BookResponse response = bookService.create(request);
        return ResponseEntity.created(URI.create("/api/books/" + response.id())).body(response);
    }

    @GetMapping
    public List<BookDtos.BookResponse> findAll() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public BookDtos.BookResponse findById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PutMapping("/{id}")
    public BookDtos.BookResponse update(@PathVariable Long id, @Valid @RequestBody BookDtos.BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
