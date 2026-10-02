package com.smartlib.ai;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.HybridBookSearchService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.entity.Book;
import com.smartlib.entity.Category;
import com.smartlib.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HybridBookSearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private QdrantVectorService qdrantVectorService;

    private GeminiAiProperties properties;
    private HybridBookSearchService searchService;

    @BeforeEach
    void setUp() {
        properties = new GeminiAiProperties();
        properties.setEmbeddingDimension(768);
        properties.setEmbeddingModel("gemini-embedding-2");

        searchService = new HybridBookSearchService(
                bookRepository,
                geminiClient,
                qdrantVectorService,
                properties
        );
    }

    private Book createBook(Long id, String title, String author, String categoryName) {
        Category cat = Category.builder().id(1L).name(categoryName).build();
        return Book.builder()
                .id(id)
                .title(title)
                .author(author)
                .category(cat)
                .isbn("1234567890")
                .publisher("Publisher")
                .publicationYear(2024)
                .description("Sample description")
                .totalCopies(3)
                .availableCopies(2)
                .averageRating(4.5)
                .build();
    }

    private List<Float> createMockVector(int size) {
        List<Float> vector = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            vector.add(0.01f);
        }
        return vector;
    }

    @Test
    @DisplayName("A. RRF scoring: Verifies exact mathematical RRF fused scores for rank 1")
    void testRrfScoringMath() {
        Book book1 = createBook(1L, "Clean Code", "Robert C. Martin", "Technology");
        ScoredPoint point1 = ScoredPoint.builder()
                .id(2L)
                .score(0.95)
                .payload(Map.of("bookId", 2L))
                .build();

        Map<Long, HybridBookSearchService.CandidateScore> fusedMap = searchService.performRrfFusion(
                List.of(book1),
                List.of(point1)
        );

        // Lexical rank 1: 1.0 / (60 + 1) = 1.0 / 61 ≈ 0.01639344
        double expectedScore = 1.0 / 61.0;

        assertEquals(2, fusedMap.size());
        assertEquals(expectedScore, fusedMap.get(1L).getFusedScore(), 0.00001);
        assertEquals(expectedScore, fusedMap.get(2L).getFusedScore(), 0.00001);
    }

    @Test
    @DisplayName("B. Duplicate fusion: Book appearing in both branches receives combined RRF score")
    void testDuplicateFusion() {
        Book book1 = createBook(10L, "Design Patterns", "GoF", "Technology");
        ScoredPoint point1 = ScoredPoint.builder()
                .id(10L)
                .score(0.92)
                .payload(Map.of("bookId", 10L))
                .build();

        Map<Long, HybridBookSearchService.CandidateScore> fusedMap = searchService.performRrfFusion(
                List.of(book1),
                List.of(point1)
        );

        // In both branches at rank 1: (1/61) + (1/61) = 2/61 ≈ 0.03278688
        double expectedScore = (1.0 / 61.0) + (1.0 / 61.0);

        assertEquals(1, fusedMap.size());
        assertEquals(expectedScore, fusedMap.get(10L).getFusedScore(), 0.00001);
        assertEquals(1, fusedMap.get(10L).getLexicalRank());
        assertEquals(1, fusedMap.get(10L).getSemanticRank());
    }

    @Test
    @DisplayName("C. Lexical-only candidate: Book found only by MySQL remains in results")
    void testLexicalOnlyCandidate() {
        Book book1 = createBook(1L, "Clean Code", "Robert C. Martin", "Technology");

        when(bookRepository.searchLexical("Clean Code")).thenReturn(List.of(book1));
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("Clean Code")).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(Collections.emptyList());
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(book1));

        List<BookSearchResult> results = searchService.search("Clean Code");

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getBookId());
        assertEquals(1, results.get(0).getLexicalRank());
        assertNull(results.get(0).getSemanticRank());
    }

    @Test
    @DisplayName("D. Semantic-only candidate: Book found only by Qdrant remains in results")
    void testSemanticOnlyCandidate() {
        Book book2 = createBook(2L, "The Pragmatic Programmer", "Andy Hunt", "Technology");
        ScoredPoint point2 = ScoredPoint.builder()
                .id(2L)
                .score(0.88)
                .payload(Map.of("bookId", 2L))
                .build();

        when(bookRepository.searchLexical("programming mastery")).thenReturn(Collections.emptyList());
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("programming mastery")).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(point2));
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(book2));

        List<BookSearchResult> results = searchService.search("programming mastery");

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getBookId());
        assertNull(results.get(0).getLexicalRank());
        assertEquals(1, results.get(0).getSemanticRank());
        assertEquals(0.88, results.get(0).getSemanticScore());
    }

    @Test
    @DisplayName("E. Hybrid ordering: Final ordering strictly follows fused scores descending")
    void testHybridOrdering() {
        // Book 1: in both branches (score ~0.032)
        // Book 2: only in lexical (score ~0.016)
        // Book 3: only in semantic rank 2 (score ~0.016)
        Book book1 = createBook(1L, "Book 1", "A", "T");
        Book book2 = createBook(2L, "Book 2", "B", "T");
        Book book3 = createBook(3L, "Book 3", "C", "T");

        ScoredPoint point1 = ScoredPoint.builder().id(1L).score(0.95).payload(Map.of("bookId", 1L)).build();
        ScoredPoint point3 = ScoredPoint.builder().id(3L).score(0.85).payload(Map.of("bookId", 3L)).build();

        when(bookRepository.searchLexical("query")).thenReturn(List.of(book1, book2));
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("query")).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(point1, point3));
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(book1, book2, book3));

        List<BookSearchResult> results = searchService.search("query");

        assertEquals(3, results.size());
        assertEquals(1L, results.get(0).getBookId(), "Book 1 must be first due to combined RRF score");
        assertTrue(results.get(0).getRelevanceScore() > results.get(1).getRelevanceScore());
        assertTrue(results.get(1).getRelevanceScore() >= results.get(2).getRelevanceScore());
    }

    @Test
    @DisplayName("F. MySQL hydration: Real MySQL data is used instead of Qdrant payload")
    void testMySqlHydrationAuthoritative() {
        Book realBook = createBook(42L, "Real MySQL Title", "Real Author", "Real Category");
        ScoredPoint stalePoint = ScoredPoint.builder()
                .id(42L)
                .score(0.99)
                .payload(Map.of("bookId", 42L, "title", "Stale Qdrant Title", "author", "Stale Author"))
                .build();

        when(bookRepository.searchLexical("test")).thenReturn(Collections.emptyList());
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("test")).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(stalePoint));
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(realBook));

        List<BookSearchResult> results = searchService.search("test");

        assertEquals(1, results.size());
        assertEquals("Real MySQL Title", results.get(0).getTitle());
        assertEquals("Real Author", results.get(0).getAuthor());
        assertEquals("Real Category", results.get(0).getCategoryName());
    }

    @Test
    @DisplayName("G. Qdrant failure fallback: When Qdrant/Gemini fails, search falls back to MySQL lexical")
    void testQdrantFailureFallback() {
        Book book1 = createBook(1L, "Clean Code", "Robert C. Martin", "Technology");

        when(bookRepository.searchLexical("Clean Code")).thenReturn(List.of(book1));
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("Clean Code")).thenThrow(new RuntimeException("Qdrant/Gemini connection failed"));
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(book1));

        List<BookSearchResult> results = searchService.search("Clean Code");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getBookId());
        assertEquals("Clean Code", results.get(0).getTitle());
    }

    @Test
    @DisplayName("H. Missing MySQL book: Stale Qdrant ID for deleted book is safely ignored")
    void testMissingMySqlBookIgnored() {
        ScoredPoint stalePoint = ScoredPoint.builder()
                .id(999L)
                .score(0.95)
                .payload(Map.of("bookId", 999L))
                .build();

        when(bookRepository.searchLexical("deleted")).thenReturn(Collections.emptyList());
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("deleted")).thenReturn(createMockVector(768));
        when(qdrantVectorService.search(anyList(), anyInt())).thenReturn(List.of(stalePoint));
        when(bookRepository.findAllById(anyList())).thenReturn(Collections.emptyList()); // Not in MySQL

        List<BookSearchResult> results = searchService.search("deleted");

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Stale Qdrant points missing from MySQL must be dropped");
    }

    @Test
    @DisplayName("M. Embedding dimension validation: Mismatched query vector dimension bypasses semantic search")
    void testInvalidQueryDimensionBypassesSemantic() {
        Book book1 = createBook(1L, "Clean Code", "Robert C. Martin", "Technology");

        when(bookRepository.searchLexical("query")).thenReturn(List.of(book1));
        when(geminiClient.isAvailable()).thenReturn(true);
        when(geminiClient.generateEmbedding("query")).thenReturn(createMockVector(512)); // 512 instead of 768
        when(bookRepository.findAllById(anyList())).thenReturn(List.of(book1));

        List<BookSearchResult> results = searchService.search("query");

        assertEquals(1, results.size());
        verify(qdrantVectorService, never()).search(anyList(), anyInt());
    }
}
