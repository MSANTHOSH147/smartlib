package com.smartlib.service;

import com.smartlib.dto.bookcopy.AdminBookCopyScanResponse;
import com.smartlib.dto.bookcopy.BookCopyResponse;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.entity.BookCopy;
import com.smartlib.entity.Borrowing;
import com.smartlib.enums.BookCopyStatus;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BookCopyRepository;
import com.smartlib.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlib.dto.bookcopy.UpdateBookCopyRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;

    private final BorrowingRepository borrowingRepository;


    // ============================================================
    // GET COPY BY QR TOKEN
    // ============================================================

    @Transactional(readOnly = true)
    public BookCopyResponse getByQrToken(String qrToken) {

        if (qrToken == null || qrToken.isBlank()) {

            throw new ResourceNotFoundException(
                    "QR token is required."
            );
        }

        BookCopy bookCopy =
                bookCopyRepository
                        .findByQrToken(qrToken)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Book copy not found for this QR code."
                                )
                        );

        return toResponse(bookCopy);
    }


    // ============================================================
    // GET ALL COPIES FOR BOOK
    // ============================================================

    @Transactional(readOnly = true)
    public List<BookCopyResponse> getByBookId(Long bookId) {

        if (bookId == null) {

            throw new ResourceNotFoundException(
                    "Book ID is required."
            );
        }

        List<BookCopy> copies =
                bookCopyRepository.findByBookId(bookId);

        return copies
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET COPY BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public BookCopyResponse getById(Long id) {

        BookCopy bookCopy =
                bookCopyRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Book copy not found."
                                )
                        );

        return toResponse(bookCopy);
    }


    // ============================================================
    // ADMIN QR SCAN
    // ============================================================

    @Transactional(readOnly = true)
    public AdminBookCopyScanResponse scanForAdmin(
            String qrToken) {

        if (qrToken == null || qrToken.isBlank()) {

            throw new ResourceNotFoundException(
                    "QR token is required."
            );
        }

        BookCopy bookCopy =
                bookCopyRepository
                        .findByQrToken(qrToken)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Book copy not found for this QR code."
                                )
                        );

        BorrowResponse activeBorrowing = null;

        boolean borrowed =
                bookCopy.getStatus()
                        == BookCopyStatus.BORROWED;

        boolean available =
                bookCopy.getStatus()
                        == BookCopyStatus.AVAILABLE;


        // ========================================================
        // FIND ACTIVE BORROWING
        // ========================================================

        if (borrowed) {

            Borrowing borrowing =
                    borrowingRepository
                            .findFirstByBookCopyIdAndStatus(
                                    bookCopy.getId(),
                                    BorrowStatus.BORROWED
                            )
                            .orElse(null);

            if (borrowing != null) {

                activeBorrowing =
                        BorrowResponse.builder()

                                .id(
                                        borrowing.getId()
                                )

                                .bookId(
                                        borrowing
                                                .getBook()
                                                .getId()
                                )

                                .bookTitle(
                                        borrowing
                                                .getBook()
                                                .getTitle()
                                )

                                .userId(
                                        borrowing
                                                .getUser()
                                                .getId()
                                )

                                .userName(
                                        borrowing
                                                .getUser()
                                                .getName()
                                )

                                .borrowDate(
                                        borrowing
                                                .getBorrowDate()
                                )

                                .dueDate(
                                        borrowing
                                                .getDueDate()
                                )

                                .returnDate(
                                        borrowing
                                                .getReturnDate()
                                )

                                .status(
                                        borrowing
                                                .getStatus()
                                                .name()
                                )

                                .build();
            }
        }


        return AdminBookCopyScanResponse
                .builder()

                .copy(
                        toResponse(bookCopy)
                )

                .activeBorrowing(
                        activeBorrowing
                )

                .borrowed(
                        borrowed
                )

                .available(
                        available
                )

                .build();
    }

    // ============================================================
// ADMIN UPDATE PHYSICAL BOOK COPY
// ============================================================

@Transactional
public BookCopyResponse updateCopy(
        Long copyId,
        UpdateBookCopyRequest request
) {

    if (copyId == null) {
        throw new ResourceNotFoundException(
                "Book copy ID is required."
        );
    }

    if (request == null) {
        throw new IllegalArgumentException(
                "Update data is required."
        );
    }

    BookCopy bookCopy =
            bookCopyRepository
                    .findById(copyId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Book copy not found."
                            )
                    );

    // --------------------------------------------------------
    // CONDITION
    // --------------------------------------------------------

    if (request.getCondition() != null) {

        bookCopy.setCondition(
                request.getCondition()
        );
    }

    // --------------------------------------------------------
    // LOCATION
    // --------------------------------------------------------

    if (request.getLocation() != null) {

        String location =
                request.getLocation().trim();

        if (location.isEmpty()) {
            bookCopy.setLocation(null);
        } else {
            bookCopy.setLocation(location);
        }
    }

    // --------------------------------------------------------
    // UPDATED TIME
    // --------------------------------------------------------

    bookCopy.setUpdatedAt(
            java.time.LocalDateTime.now()
    );

    BookCopy savedCopy =
            bookCopyRepository.save(bookCopy);

    return toResponse(savedCopy);
}


    // ============================================================
    // ENTITY → RESPONSE
    // ============================================================

    private BookCopyResponse toResponse(
            BookCopy bookCopy) {

        return BookCopyResponse
                .builder()

                .id(
                        bookCopy.getId()
                )

                .bookId(
                        bookCopy
                                .getBook()
                                .getId()
                )

                .bookTitle(
                        bookCopy
                                .getBook()
                                .getTitle()
                )

                .author(
                        bookCopy
                                .getBook()
                                .getAuthor()
                )

                .isbn(
                        bookCopy
                                .getBook()
                                .getIsbn()
                )

                .copyNumber(
                        bookCopy.getCopyNumber()
                )

                .status(
                        bookCopy
                                .getStatus()
                                .name()
                )

                .condition(
                        bookCopy
                                .getCondition()
                                .name()
                )

                .location(
                        bookCopy.getLocation()
                )

                .qrToken(
                        bookCopy.getQrToken()
                )

                .createdAt(
                        bookCopy.getCreatedAt()
                )

                .updatedAt(
                        bookCopy.getUpdatedAt()
                )

                .build();
    }
}