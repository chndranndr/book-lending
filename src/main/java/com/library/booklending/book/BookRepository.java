package com.library.booklending.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE book
                    SET available_copies = available_copies - 1
                    WHERE id = :bookId
                      AND available_copies > 0
                    """,
            nativeQuery = true
    )
    int decrementAvailableCopiesIfAvailable(@Param("bookId") Long bookId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE book
                    SET available_copies = available_copies + 1
                    WHERE id = :bookId
                    """,
            nativeQuery = true
    )
    int incrementAvailableCopies(@Param("bookId") Long bookId);
}
