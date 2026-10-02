package com.smartlib.ai;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.PersonalizedRecommendationService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.entity.Book;
import com.smartlib.entity.Borrowing;
import com.smartlib.entity.Category;
import com.smartlib.entity.User;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.enums.Role;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PersonalizedRecommendationServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private QdrantVectorService qdrantVectorService;

    private GeminiAiProperties properties;
    private PersonalizedRecommendationService recommendationService;

    private User testUser;
    private Category techCategory;

    @BeforeEach
    void setUp() {
        properties = new GeminiAiProperties();
        properties.setEmbeddingDimension(768);
        properties.setEmbeddingModel("gemini-embedding-2");

        recommendationService = new PersonalizedRecommendationService(
                borrowingRepository,
                bookRepository,
                geminiClient,
                qdrantVectorService,
                properties
        );

        testUser = User.builder()
                .id(100L)
                .name("Alice")
                .email("alice@smartlib.com")
                .role(Role.MEMBER)
                .build();

        techCategory = Category.builder()
                .id(1L)
                .name("Technology")
                .build();
    }

    private Book createBook(Long id, String title, String author, int availableCopies, double rating) {
        return Book.builder()
                .id(id)
                .title(title)
                .author(author)
                .category(techCategory)
                .availableCopies(availableCopies)
                .totalCopies(availableCopies + 1)
                .averageRating(rating)
                .description("Sample description")
                .build();
    }

    private Borrowing createBorrowing(Long id, Book book, BorrowStatus status) {
        return Borrowing.builder()
                .id(id)
                .user(testUser)
                .book(book)
                .borrowDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(4))
                .status(status)
                .build();
    }

    private List<Float> createMockVector(int size) {
        List<Float> vector = new ArrayList<>(size);
        for (int i = 0; i < size; i++) vector.add(0.01f);
        return vector;
    }

    @Test
    @DisplayName("I. Recommendation history filtering: Books already borrowed by member are excluded")
    void testAlreadyBorrowedBooksExcluded() {
        Book borrowedBook = createBook(1L, "Clean Code", "Robert C. Martin", 2, 4.8);
        Book unborrowedBook = createBook(2L, "The Pragmatic Programmer", "Andy Hunt", 1, 4.7);

        Borrowing borrowing = createBorrowing(10L, borrowedBook, BorrowStatus.RETURNED);
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(borrowing));

        // Qdrant returns both candidate points (1 and 2)
        ScoredPoint point1 = ScoredPoint.builder().id(1L).score(0.95).payload(Map.of("bookId", 1L)).build();
        ScoredPoint point2 = ScoredPoint.builder().id(2L).score(0.90).payload(Map.of("bookId", 2L)).build();

        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(point1, point2));
        when(bookRepository.findAllById(anyCollection())).thenReturn(List.of(unborrowedBook));

        List<PersonalizedRecommendation> recs = recommendationService.getRecommendations(testUser, 5);

        assertNotNull(recs);
        assertEquals(1, recs.size());
        assertEquals(2L, recs.get(0).getBookId(), "Borrowed book 1 must be filtered out");
        assertEquals("The Pragmatic Programmer", recs.get(0).getTitle());
    }

    @Test
    @DisplayName("J. Recommendation availability: Available books receive availability signal and boost")
    void testRecommendationAvailabilitySignal() {
        Book availableBook = createBook(10L, "Book A", "Author A", 3, 4.5);
        Book unavailableBook = createBook(20L, "Book B", "Author B", 0, 4.5);

        Book priorBook = createBook(99L, "Prior", "Author X", 1, 4.0);
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(createBorrowing(1L, priorBook, BorrowStatus.RETURNED)));

        // Equal similarity (0.8) and equal rating (4.5)
        ScoredPoint point1 = ScoredPoint.builder().id(10L).score(0.8).payload(Map.of("bookId", 10L)).build();
        ScoredPoint point2 = ScoredPoint.builder().id(20L).score(0.8).payload(Map.of("bookId", 20L)).build();

        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(point1, point2));
        when(bookRepository.findAllById(anyCollection())).thenReturn(List.of(availableBook, unavailableBook));

        List<PersonalizedRecommendation> recs = recommendationService.getRecommendations(testUser, 5);

        assertEquals(2, recs.size());
        PersonalizedRecommendation first = recs.get(0);
        PersonalizedRecommendation second = recs.get(1);

        assertEquals(10L, first.getBookId(), "Available book must rank higher due to availability boost");
        assertTrue(first.isAvailable());
        assertTrue(first.getReasonSignals().contains("Available to borrow now"));

        assertEquals(20L, second.getBookId());
        assertFalse(second.isAvailable());
        assertTrue(second.getReasonSignals().contains("Currently on loan"));
    }

    @Test
    @DisplayName("K. Deterministic ranking: Identical candidate data produces reproducible rankings and scores")
    void testDeterministicRankingCalculation() {
        Book priorBook = createBook(99L, "Prior", "Author X", 1, 4.0);
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(createBorrowing(1L, priorBook, BorrowStatus.RETURNED)));

        Book candidate = createBook(50L, "Refactoring", "Martin Fowler", 2, 4.5); // available (boost=0.2), rating=4.5/5.0=0.9 (0.18)
        ScoredPoint point = ScoredPoint.builder().id(50L).score(0.90).payload(Map.of("bookId", 50L)).build(); // similarity 0.9 * 0.6 = 0.54

        // Expected score: 0.54 + 0.18 + 0.20 = 0.92
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(point));
        when(bookRepository.findAllById(anyCollection())).thenReturn(List.of(candidate));

        List<PersonalizedRecommendation> recs = recommendationService.getRecommendations(testUser, 5);

        assertEquals(1, recs.size());
        assertEquals(0.92, recs.get(0).getRelevanceScore(), 0.001);
    }

    @Test
    @DisplayName("L. Empty borrowing history: Gracefully falls back to top-rated available books")
    void testEmptyBorrowingHistoryFallback() {
        when(borrowingRepository.findByUserId(100L)).thenReturn(Collections.emptyList());

        Book popularBook1 = createBook(1L, "Popular Tech 1", "Author 1", 2, 4.9);
        Book popularBook2 = createBook(2L, "Popular Tech 2", "Author 2", 1, 4.6);

        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(List.of(popularBook1, popularBook2));

        List<PersonalizedRecommendation> recs = recommendationService.getRecommendations(testUser, 5);

        assertNotNull(recs);
        assertEquals(2, recs.size());
        assertEquals(1L, recs.get(0).getBookId());
        assertEquals("Popular Tech 1", recs.get(0).getTitle());
        assertTrue(recs.get(0).getReasonSignals().contains("Popular library recommendation"));
        verify(qdrantVectorService, never()).search(anyList(), anyInt());
    }

    @Test
    @DisplayName("Stale Qdrant candidate missing from MySQL is safely dropped")
    void testStaleQdrantCandidateDropped() {
        Book priorBook = createBook(99L, "Prior", "Author X", 1, 4.0);
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(createBorrowing(1L, priorBook, BorrowStatus.RETURNED)));

        ScoredPoint stalePoint = ScoredPoint.builder().id(999L).score(0.99).payload(Map.of("bookId", 999L)).build();

        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(stalePoint));
        when(bookRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList()); // Not in MySQL

        List<PersonalizedRecommendation> recs = recommendationService.getRecommendations(testUser, 5);

        assertNotNull(recs);
        assertTrue(recs.isEmpty(), "Missing MySQL book must be safely dropped");
    }
}
