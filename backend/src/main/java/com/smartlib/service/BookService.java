package com.smartlib.service;

import com.smartlib.dto.book.BookRequest;
import com.smartlib.dto.book.BookResponse;
import com.smartlib.entity.Book;
import com.smartlib.entity.Category;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: " + id
                        ));

        return toResponse(book);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> searchBooks(String query) {

        if (query == null || query.isBlank()) {
            return getAllBooks();
        }

        return bookRepository
                .findByTitleContainingIgnoreCase(query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooksByAuthor(String author) {

        return bookRepository
                .findByAuthorContainingIgnoreCase(author)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooksByCategory(Long categoryId) {

        return bookRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAvailableBooks() {

        return bookRepository
                .findByAvailableCopiesGreaterThan(0)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: "
                                        + request.getCategoryId()
                        ));

        if (request.getIsbn() != null &&
                !request.getIsbn().isBlank()) {

            if (bookRepository.existsByIsbn(request.getIsbn())) {
                throw new BadRequestException(
                        "A book with this ISBN already exists"
                );
            }
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .category(category)
                .build();

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateBook(
            Long id,
            BookRequest request) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: " + id
                        ));

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"
                        ));

        int borrowedCopies =
                book.getTotalCopies()
                        - book.getAvailableCopies();

        if (request.getTotalCopies() < borrowedCopies) {
            throw new BadRequestException(
                    "Total copies cannot be less than borrowed copies"
            );
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setDescription(request.getDescription());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setTotalCopies(request.getTotalCopies());

        book.setAvailableCopies(
                request.getTotalCopies() - borrowedCopies
        );

        book.setCategory(category);

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: " + id
                        ));

        bookRepository.delete(book);
    }

    private BookResponse toResponse(Book book) {

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .averageRating(book.getAverageRating())
                .totalRatings(book.getTotalRatings())
                .categoryId(book.getCategory().getId())
                .categoryName(book.getCategory().getName())
                .build();
    }
}