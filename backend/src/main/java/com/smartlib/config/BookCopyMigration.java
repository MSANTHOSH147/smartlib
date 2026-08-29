package com.smartlib.config;

import com.smartlib.entity.Book;
import com.smartlib.entity.BookCopy;
import com.smartlib.entity.Borrowing;
import com.smartlib.enums.BookCopyCondition;
import com.smartlib.enums.BookCopyStatus;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.repository.BookCopyRepository;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class BookCopyMigration {

    @Bean
    CommandLineRunner migrateBookCopies(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            BorrowingRepository borrowingRepository) {

        return args -> migrate(
                bookRepository,
                bookCopyRepository,
                borrowingRepository
        );
    }

    @Transactional
    public void migrate(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            BorrowingRepository borrowingRepository) {

        System.out.println(
                "========================================"
        );

        System.out.println(
                "SmartLib BookCopy migration started"
        );

        // =====================================================
        // 1. CREATE MISSING COPIES
        // =====================================================

        List<Book> books = bookRepository.findAll();

        for (Book book : books) {

            long existingCopies =
                    bookCopyRepository.countByBookId(
                            book.getId()
                    );

            int requiredCopies =
                    book.getTotalCopies();

            if (existingCopies >= requiredCopies) {
                continue;
            }

            for (
                    int copyIndex = (int) existingCopies + 1;
                    copyIndex <= requiredCopies;
                    copyIndex++
            ) {

                String copyNumber =
                        String.format(
                                "COPY-%03d",
                                copyIndex
                        );

                if (
                        bookCopyRepository
                                .existsByBookIdAndCopyNumber(
                                        book.getId(),
                                        copyNumber
                                )
                ) {
                    continue;
                }

                BookCopy copy =
                        BookCopy.builder()
                                .book(book)
                                .copyNumber(copyNumber)
                                .status(
                                        BookCopyStatus.AVAILABLE
                                )
                                .condition(
                                        BookCopyCondition.GOOD
                                )
                                .location("LIBRARY")
                                .qrToken(UUID.randomUUID().toString())
                                .build();

                bookCopyRepository.save(copy);
            }
        }

        // =====================================================
        // 2. ASSIGN EXISTING BORROWINGS
        // =====================================================

        List<Borrowing> borrowings =
                borrowingRepository.findAll();

        for (Borrowing borrowing : borrowings) {

            // Already migrated
            if (borrowing.getBookCopy() != null) {
                continue;
            }

            Book book = borrowing.getBook();

            List<BookCopy> copies =
                    bookCopyRepository.findByBookId(
                            book.getId()
                    );

            if (copies.isEmpty()) {
                throw new IllegalStateException(
                        "No copies found for book ID "
                                + book.getId()
                );
            }

            // -------------------------------------------------
            // Find a copy that isn't already assigned to
            // another active borrowing.
            // -------------------------------------------------

            Set<Long> activeCopyIds =
                    borrowings.stream()
                            .filter(
                                    other ->
                                            other.getBookCopy() != null
                                                    &&
                                            other.getStatus() !=
                                                    BorrowStatus.RETURNED
                            )
                            .filter(
                                    other ->
                                            other.getBook()
                                                    .getId()
                                                    .equals(
                                                            book.getId()
                                                    )
                            )
                            .map(
                                    other ->
                                            other.getBookCopy()
                                                    .getId()
                            )
                            .collect(Collectors.toSet());

            BookCopy selectedCopy =
                    copies.stream()
                            .filter(
                                    copy ->
                                            !activeCopyIds.contains(
                                                    copy.getId()
                                            )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Not enough physical copies "
                                                    + "for book: "
                                                    + book.getTitle()
                                    )
                            );

            borrowing.setBookCopy(selectedCopy);

            // -------------------------------------------------
            // Set physical copy status
            // -------------------------------------------------

            if (
                    borrowing.getStatus() ==
                            BorrowStatus.BORROWED
                            ||
                    borrowing.getStatus() ==
                            BorrowStatus.OVERDUE
            ) {

                selectedCopy.setStatus(
                        BookCopyStatus.BORROWED
                );

            } else {

                selectedCopy.setStatus(
                        BookCopyStatus.AVAILABLE
                );
            }

            bookCopyRepository.save(selectedCopy);
            borrowingRepository.save(borrowing);
        }

        // =====================================================
        // 3. RECALCULATE AVAILABLE COPIES
        // =====================================================

        for (Book book : books) {

            long activeCount =
                    bookCopyRepository
                            .countByBookIdAndStatus(
                                    book.getId(),
                                    BookCopyStatus.BORROWED
                            );

            int available =
                    book.getTotalCopies()
                            - (int) activeCount;

            if (available < 0) {
                available = 0;
            }

            book.setAvailableCopies(
                    available
            );

            bookRepository.save(book);
        }

        System.out.println(
                "SmartLib BookCopy migration completed"
        );

        System.out.println(
                "========================================"
        );
    }
}