package com.library.booklending.book;

import com.library.booklending.common.BusinessErrorCode;
import com.library.booklending.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookDtos.BookResponse create(BookDtos.BookRequest request) {
        Book book = new Book(
                request.title(),
                request.author(),
                request.isbn(),
                request.totalCopies(),
                request.totalCopies()
        );
        return toResponse(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public List<BookDtos.BookResponse> findAll() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookDtos.BookResponse findById(Long id) {
        return toResponse(getBook(id));
    }

    @Transactional
    public BookDtos.BookResponse update(Long id, BookDtos.BookRequest request) {
        Book book = getBook(id);
        int borrowedCopies = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < borrowedCopies) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Invalid total copies",
                    "Requested totalCopies is below the number of borrowed copies",
                    BusinessErrorCode.INVALID_TOTAL_COPIES
            );
        }

        int availableCopies = request.totalCopies() - borrowedCopies;
        book.update(
                request.title(),
                request.author(),
                request.isbn(),
                request.totalCopies(),
                availableCopies
        );
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void delete(Long id) {
        Book book = getBook(id);
        bookRepository.delete(book);
        bookRepository.flush();
    }

    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Book not found",
                        "Book " + id + " does not exist",
                        BusinessErrorCode.BOOK_NOT_FOUND
                ));
    }

    public BookDtos.BookResponse toResponse(Book book) {
        return new BookDtos.BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies()
        );
    }
}
