package com.smartlib.service;

import com.smartlib.dto.borrowing.BorrowRequest;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.entity.Book;
import com.smartlib.entity.BookCopy;
import com.smartlib.entity.Borrowing;
import com.smartlib.entity.Reservation;
import com.smartlib.entity.User;
import com.smartlib.enums.BookCopyStatus;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.enums.ReservationStatus;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BookCopyRepository;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import com.smartlib.repository.ReservationRepository;
import com.smartlib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;


    // ============================================================
    // NORMAL BORROW
    // ============================================================

    @Transactional
    public BorrowResponse borrowBook(
            BorrowRequest request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Book book = bookRepository
                .findById(request.getBookId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found."
                        ));

        return borrowAvailableCopy(
                user,
                book
        );
    }


    // ============================================================
    // QR BORROW
    // POST /api/borrowings/qr
    // ============================================================

    @Transactional
    public BorrowResponse borrowBookByQr(
            String qrToken,
            Authentication authentication) {

        System.out.println("=================================");
        System.out.println("SMARTLIB QR BORROW START");
        System.out.println("QR TOKEN = " + qrToken);

        // --------------------------------------------------------
        // Validate QR
        // --------------------------------------------------------

        if (qrToken == null || qrToken.isBlank()) {

            throw new BadRequestException(
                    "QR token is required."
            );
        }

        // --------------------------------------------------------
        // Authenticate member
        // --------------------------------------------------------

        User user = getCurrentUser(authentication);

        System.out.println(
                "QR BORROW USER = " +
                user.getId() +
                " / " +
                user.getEmail()
        );

        // --------------------------------------------------------
        // Find EXACT physical copy
        // --------------------------------------------------------

        BookCopy bookCopy =
                bookCopyRepository
                        .findByQrToken(qrToken.trim())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Book copy not found for QR token."
                                )
                        );

        System.out.println(
                "QR COPY ID = " +
                bookCopy.getId()
        );

        System.out.println(
                "QR COPY NUMBER = " +
                bookCopy.getCopyNumber()
        );

        System.out.println(
                "QR COPY STATUS = " +
                bookCopy.getStatus()
        );

        // --------------------------------------------------------
        // Get book
        // --------------------------------------------------------

        Book book = bookCopy.getBook();

        if (book == null) {

            throw new BadRequestException(
                    "This physical copy is not linked to a book."
            );
        }

        System.out.println(
                "QR BOOK ID = " +
                book.getId()
        );

        System.out.println(
                "QR BOOK TITLE = " +
                book.getTitle()
        );

        // --------------------------------------------------------
        // Physical copy must be available
        // --------------------------------------------------------

        if (bookCopy.getStatus()
                != BookCopyStatus.AVAILABLE) {

            throw new BadRequestException(
                    "This physical book copy is not available."
            );
        }

        // --------------------------------------------------------
        // Prevent duplicate borrowing
        // --------------------------------------------------------

        boolean alreadyBorrowed =
                borrowingRepository
                        .existsByUserIdAndBookIdAndStatus(
                                user.getId(),
                                book.getId(),
                                BorrowStatus.BORROWED
                        );

        if (alreadyBorrowed) {

            throw new BadRequestException(
                    "You have already borrowed this book."
            );
        }

        // --------------------------------------------------------
        // BORROW EXACT SCANNED COPY
        // --------------------------------------------------------

        bookCopy.setStatus(
                BookCopyStatus.BORROWED
        );

        bookCopy.setUpdatedAt(
                LocalDateTime.now()
        );

        bookCopyRepository.save(bookCopy);

        System.out.println(
                "QR COPY UPDATED TO BORROWED"
        );

        // --------------------------------------------------------
        // Create borrowing
        // --------------------------------------------------------

        LocalDate borrowDate =
                LocalDate.now();

        LocalDate dueDate =
                borrowDate.plusDays(14);

        Borrowing borrowing =
                Borrowing.builder()
                        .user(user)
                        .book(book)
                        .bookCopy(bookCopy)
                        .borrowDate(borrowDate)
                        .dueDate(dueDate)
                        .status(BorrowStatus.BORROWED)
                        .createdAt(LocalDateTime.now())
                        .build();

        Borrowing saved =
                borrowingRepository.save(
                        borrowing
                );

        System.out.println(
                "QR BORROWING CREATED ID = " +
                saved.getId()
        );

        // --------------------------------------------------------
        // Update book available counter
        // --------------------------------------------------------

        long available =
                bookCopyRepository
                        .countByBookIdAndStatus(
                                book.getId(),
                                BookCopyStatus.AVAILABLE
                        );

        book.setAvailableCopies(
                (int) available
        );

        book.setUpdatedAt(
                LocalDateTime.now()
        );

        bookRepository.save(book);

        System.out.println(
                "BOOK AVAILABLE COPIES = " +
                available
        );

        System.out.println("SMARTLIB QR BORROW SUCCESS");
        System.out.println("=================================");

        return toResponse(saved);
    }


    // ============================================================
    // NORMAL AVAILABLE COPY BORROW
    // ============================================================

    private BorrowResponse borrowAvailableCopy(
            User user,
            Book book) {

        // --------------------------------------------------------
        // Prevent duplicate
        // --------------------------------------------------------

        boolean alreadyBorrowed =
                borrowingRepository
                        .existsByUserIdAndBookIdAndStatus(
                                user.getId(),
                                book.getId(),
                                BorrowStatus.BORROWED
                        );

        if (alreadyBorrowed) {

            throw new BadRequestException(
                    "You have already borrowed this book."
            );
        }

        // --------------------------------------------------------
        // Find available physical copy
        // --------------------------------------------------------

        List<BookCopy> availableCopies =
                bookCopyRepository
                        .findAvailableCopiesForUpdate(
                                book.getId(),
                                BookCopyStatus.AVAILABLE
                        );

        if (availableCopies.isEmpty()) {

            throw new BadRequestException(
                    "Book is currently unavailable. Please reserve it."
            );
        }

        BookCopy bookCopy =
                availableCopies.get(0);

        // --------------------------------------------------------
        // Mark physical copy borrowed
        // --------------------------------------------------------

        bookCopy.setStatus(
                BookCopyStatus.BORROWED
        );

        bookCopy.setUpdatedAt(
                LocalDateTime.now()
        );

        bookCopyRepository.save(bookCopy);

        // --------------------------------------------------------
        // Create borrowing
        // --------------------------------------------------------

        LocalDate borrowDate =
                LocalDate.now();

        LocalDate dueDate =
                borrowDate.plusDays(14);

        Borrowing borrowing =
                Borrowing.builder()
                        .user(user)
                        .book(book)
                        .bookCopy(bookCopy)
                        .borrowDate(borrowDate)
                        .dueDate(dueDate)
                        .status(BorrowStatus.BORROWED)
                        .createdAt(LocalDateTime.now())
                        .build();

        Borrowing saved =
                borrowingRepository.save(
                        borrowing
                );

        updateBookAvailableCopies(book);

        return toResponse(saved);
    }


    // ============================================================
    // MY BORROWINGS
    // ============================================================

    @Transactional(readOnly = true)
    public List<BorrowResponse> getMyBorrowings(
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);

        return borrowingRepository
                .findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // MY ACTIVE BORROWINGS
    // ============================================================

    @Transactional(readOnly = true)
    public List<BorrowResponse> getMyActiveBorrowings(
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);

        return borrowingRepository
                .findByUserIdAndStatus(
                        user.getId(),
                        BorrowStatus.BORROWED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // NORMAL RETURN
    // ============================================================

    @Transactional
    public BorrowResponse returnBook(
            Long borrowingId,
            Authentication authentication) {

        User user =
                getCurrentUser(authentication);

        Borrowing borrowing =
                borrowingRepository
                        .findById(borrowingId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Borrowing not found."
                                ));

        if (!borrowing
                .getUser()
                .getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    "You are not allowed to return this borrowing."
            );
        }

        return completeReturn(borrowing);
    }

    // ============================================================
// ADMIN QR RETURN
// POST /api/admin/book-copies/qr/return
// ============================================================




    // ============================================================
    // QR RETURN
    // ============================================================

    @Transactional
    public BorrowResponse returnBookByQr(
            String qrToken,
            Authentication authentication) {

        if (qrToken == null || qrToken.isBlank()) {

            throw new BadRequestException(
                    "QR token is required."
            );
        }

        User user =
                getCurrentUser(authentication);

        BookCopy bookCopy =
                bookCopyRepository
                        .findByQrToken(qrToken.trim())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Book copy not found for QR token."
                                )
                        );

        Borrowing borrowing =
                borrowingRepository
                        .findByUserIdAndStatus(
                                user.getId(),
                                BorrowStatus.BORROWED
                        )
                        .stream()
                        .filter(b ->
                                b.getBookCopy() != null
                                        && b.getBookCopy()
                                        .getId()
                                        .equals(bookCopy.getId())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "You do not have this physical copy borrowed."
                                )
                        );

        return completeReturn(borrowing);
    }


    // ============================================================
    // COMPLETE RETURN
    // ============================================================

    private BorrowResponse completeReturn(
            Borrowing borrowing) {

        Book book =
                borrowing.getBook();

        BookCopy bookCopy =
                borrowing.getBookCopy();

        if (bookCopy == null) {

            throw new BadRequestException(
                    "This borrowing has no physical book copy."
            );
        }

        // --------------------------------------------------------
        // Return copy
        // --------------------------------------------------------

        bookCopy.setStatus(
                BookCopyStatus.AVAILABLE
        );

        bookCopy.setUpdatedAt(
                LocalDateTime.now()
        );

        bookCopyRepository.save(bookCopy);

        // --------------------------------------------------------
        // Return borrowing
        // --------------------------------------------------------

        borrowing.setReturnDate(
                LocalDate.now()
        );

        borrowing.setStatus(
                BorrowStatus.RETURNED
        );

        Borrowing saved =
                borrowingRepository.save(
                        borrowing
                );

        updateBookAvailableCopies(book);

        return toResponse(saved);
    }


    // ============================================================
    // UPDATE AVAILABLE COPIES
    // ============================================================

    private void updateBookAvailableCopies(
            Book book) {

        long available =
                bookCopyRepository
                        .countByBookIdAndStatus(
                                book.getId(),
                                BookCopyStatus.AVAILABLE
                        );

        book.setAvailableCopies(
                (int) available
        );

        book.setUpdatedAt(
                LocalDateTime.now()
        );

        bookRepository.save(book);
    }


    // ============================================================
    // CURRENT USER
    // ============================================================

    private User getCurrentUser(
        Authentication authentication) {

    if (authentication == null
            || !authentication.isAuthenticated()) {

        throw new BadRequestException(
                "Authentication is required."
        );
    }

    // ============================================================
    // JWT FILTER STORES THE ACTUAL USER AS THE PRINCIPAL
    // ============================================================

    Object principal =
            authentication.getPrincipal();

    if (principal instanceof User) {

        User user = (User) principal;

        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new BadRequestException(
                    "User account is inactive."
            );
        }

        return user;
    }

    // ============================================================
    // FALLBACK
    // ============================================================

    String email =
            authentication.getName();

    if (email == null || email.isBlank()) {

        throw new BadRequestException(
                "Authentication user information is missing."
        );
    }

    User user =
            userRepository
                    .findByEmail(email)
                    .orElse(null);

    if (user == null) {

        throw new ResourceNotFoundException(
                "Authenticated user not found."
        );
    }

    if (!Boolean.TRUE.equals(user.getActive())) {

        throw new BadRequestException(
                "User account is inactive."
        );
    }

    return user;
}


    // ============================================================
    // ENTITY -> RESPONSE
    // ============================================================

    private BorrowResponse toResponse(
            Borrowing borrowing) {

        return BorrowResponse
                .builder()
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
                        borrowing.getBorrowDate()
                )
                .dueDate(
                        borrowing.getDueDate()
                )
                .returnDate(
                        borrowing.getReturnDate()
                )
                .status(
                        borrowing
                                .getStatus()
                                .name()
                )
                .build();
    }

    // ============================================================
// ADMIN QR RETURN
// POST /api/admin/book-copies/qr/return
// ============================================================

@Transactional
public BorrowResponse returnBookByAdminQr(String qrToken) {

    if (qrToken == null || qrToken.isBlank()) {
        throw new BadRequestException(
                "QR token is required."
        );
    }

    String token = qrToken.trim();

    System.out.println("========================================");
    System.out.println("SMARTLIB ADMIN QR RETURN");
    System.out.println("QR TOKEN: " + token);

    // ------------------------------------------------------------
    // 1. FIND PHYSICAL COPY
    // ------------------------------------------------------------

    BookCopy bookCopy =
            bookCopyRepository
                    .findByQrToken(token)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Book copy not found for QR token."
                            )
                    );

    System.out.println(
            "COPY: " + bookCopy.getCopyNumber()
    );

    System.out.println(
            "COPY ID: " + bookCopy.getId()
    );

    // ------------------------------------------------------------
    // 2. VERIFY COPY IS BORROWED
    // ------------------------------------------------------------

    if (bookCopy.getStatus() != BookCopyStatus.BORROWED) {

        throw new BadRequestException(
                "This physical copy is not currently borrowed."
        );
    }

    // ------------------------------------------------------------
    // 3. FIND ACTIVE BORROWING
    // ------------------------------------------------------------

    Borrowing borrowing =
            borrowingRepository
                    .findByBookCopyIdAndStatus(
                            bookCopy.getId(),
                            BorrowStatus.BORROWED
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "No active borrowing found for this physical copy."
                            )
                    );

    System.out.println(
            "BORROWING ID: " + borrowing.getId()
    );

    // ------------------------------------------------------------
    // 4. RETURN PHYSICAL COPY
    // ------------------------------------------------------------

    bookCopy.setStatus(
            BookCopyStatus.AVAILABLE
    );

    bookCopy.setUpdatedAt(
            LocalDateTime.now()
    );

    bookCopyRepository.save(bookCopy);

    // ------------------------------------------------------------
    // 5. COMPLETE BORROWING
    // ------------------------------------------------------------

    borrowing.setReturnDate(
            LocalDate.now()
    );

    borrowing.setStatus(
            BorrowStatus.RETURNED
    );

    Borrowing savedBorrowing =
            borrowingRepository.save(
                    borrowing
            );

    // ------------------------------------------------------------
    // 6. UPDATE BOOK AVAILABILITY
    // ------------------------------------------------------------

    Book book = borrowing.getBook();

    updateBookAvailableCopies(book);

    // ------------------------------------------------------------
    // 7. RESPONSE
    // ------------------------------------------------------------

    System.out.println(
            "ADMIN QR RETURN SUCCESS"
    );

    System.out.println("========================================");

    return toResponse(savedBorrowing);
}
// ============================================================
// ADMIN DEACTIVATE PHYSICAL BOOK COPY
// ============================================================

@Transactional
public void deactivateBookCopy(Long bookCopyId) {

    if (bookCopyId == null) {
        throw new IllegalArgumentException(
                "Book copy ID is required."
        );
    }

    BookCopy bookCopy =
            bookCopyRepository
                    .findById(bookCopyId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Book copy not found."
                            )
                    );

    // --------------------------------------------------------
    // NEVER DEACTIVATE A CURRENTLY BORROWED COPY
    // --------------------------------------------------------

    if (bookCopy.getStatus() == BookCopyStatus.BORROWED) {

        throw new BadRequestException(
                "Cannot deactivate a currently borrowed book copy."
        );
    }

    // --------------------------------------------------------
    // ALREADY DEACTIVATED
    // --------------------------------------------------------

    if (bookCopy.getStatus() == BookCopyStatus.DEACTIVATED) {

        throw new BadRequestException(
                "This book copy is already deactivated."
        );
    }

    // --------------------------------------------------------
    // DEACTIVATE
    // --------------------------------------------------------

    bookCopy.setStatus(
            BookCopyStatus.DEACTIVATED
    );

    bookCopy.setUpdatedAt(
            java.time.LocalDateTime.now()
    );

    bookCopyRepository.save(bookCopy);
}
}