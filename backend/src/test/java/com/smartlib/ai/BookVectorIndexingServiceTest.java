package com.smartlib.ai;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.IndexingResult;
import com.smartlib.ai.dto.qdrant.PointStruct;
import com.smartlib.ai.service.BookVectorIndexingService;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.entity.Book;
import com.smartlib.entity.Category;
import com.smartlib.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookVectorIndexingServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private QdrantVectorService qdrantVectorService;

    private GeminiAiProperties properties;
    private BookVectorIndexingService indexingService;

    @BeforeEach
    void setUp() {
        properties = new GeminiAiProperties();
        properties.setEmbeddingDimension(768);
        properties.setEmbeddingModel("gemini-embedding-2");

        indexingService = new BookVectorIndexingService(
                bookRepository,
                geminiClient,
                qdrantVectorService,
                properties
        );
    }

    private Book createSampleBook(Long id, String title, String author, String categoryName, String description) {
        Category category = Category.builder()
                .id(1L)
                .name(categoryName)
                .build();

        return Book.builder()
                .id(id)
                .title(title)
                .author(author)
                .category(category)
                .publisher("Prentice Hall")
                .publicationYear(2008)
                .isbn("978-0132350884")
                .description(description)
                .build();
    }

    private List<Float> createMockVector(int size) {
        List<Float> vector = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            vector.add(0.01f * (i + 1));
        }
        return vector;
    }

    @Test
    @DisplayName("A. Searchable text generation contains title, author, category, description")
    void testBuildSearchableText() {
        Book book = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "A handbook of agile software craftsmanship.");

        String text = indexingService.buildSearchableText(book);

        assertTrue(text.contains("Title: Clean Code"));
        assertTrue(text.contains("Author: Robert C. Martin"));
        assertTrue(text.contains("Category: Technology"));
        assertTrue(text.contains("Publisher: Prentice Hall"));
        assertTrue(text.contains("Publication Year: 2008"));
        assertTrue(text.contains("ISBN: 978-0132350884"));
        assertTrue(text.contains("Description: A handbook of agile software craftsmanship."));
    }

    @Test
    @DisplayName("B. SHA-256 determinism: Same book data produces identical hash")
    void testContentHashDeterminism() {
        Book book1 = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "A handbook of agile craftsmanship.");
        Book book2 = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "A handbook of agile craftsmanship.");

        String hash1 = indexingService.calculateContentHash(book1);
        String hash2 = indexingService.calculateContentHash(book2);

        assertNotNull(hash1);
        assertFalse(hash1.isBlank());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("C. SHA-256 change detection: Changing description produces different hash")
    void testContentHashChangeDetection() {
        Book book1 = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "A handbook of agile craftsmanship.");
        Book book2 = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "An updated guide to clean code.");

        String hash1 = indexingService.calculateContentHash(book1);
        String hash2 = indexingService.calculateContentHash(book2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("D. Embedding dimension validation: 768 vector accepted, mismatched dimension rejected")
    void testEmbeddingDimensionValidation() {
        Book book = createSampleBook(1L, "Clean Code", "Robert C. Martin", "Technology", "Desc");

        // 1. Correct 768 dimension -> accepted
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.upsertPoints(anyList())).thenReturn(true);

        assertDoesNotThrow(() -> indexingService.indexBook(book));

        // 2. Incorrect dimension (e.g. 512) -> rejected
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(512));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                indexingService.indexBook(book)
        );
        assertTrue(ex.getMessage().contains("expected 768, got 512"));
        verify(qdrantVectorService, times(1)).upsertPoints(anyList()); // Not called again
    }

    @Test
    @DisplayName("E. New book indexing: Missing vector in Qdrant triggers Gemini embedding and Qdrant upsert")
    void testNewBookIndexing() {
        Book book = createSampleBook(10L, "The Pragmatic Programmer", "Andy Hunt", "Technology", "Your journey to mastery.");

        when(qdrantVectorService.getPoint(10L)).thenReturn(null);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.upsertPoints(anyList())).thenReturn(true);

        boolean indexed = indexingService.indexBookIfChanged(book);

        assertTrue(indexed);
        verify(geminiClient, times(1)).generateEmbedding(anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointStruct>> captor = ArgumentCaptor.forClass(List.class);
        verify(qdrantVectorService).upsertPoints(captor.capture());

        List<PointStruct> upserted = captor.getValue();
        assertEquals(1, upserted.size());
        assertEquals(10L, upserted.get(0).getId());
        assertEquals(768, upserted.get(0).getVector().size());
        assertEquals("The Pragmatic Programmer", upserted.get(0).getPayload().get("title"));
    }

    @Test
    @DisplayName("F. Unchanged book: Same contentHash in Qdrant skips Gemini embedding API call")
    void testUnchangedBookSkipped() {
        Book book = createSampleBook(20L, "Refactoring", "Martin Fowler", "Technology", "Improving existing code.");
        String expectedHash = indexingService.calculateContentHash(book);

        PointStruct existingPoint = PointStruct.of(20L, createMockVector(768), Map.of("contentHash", expectedHash));
        when(qdrantVectorService.getPoint(20L)).thenReturn(existingPoint);

        boolean indexed = indexingService.indexBookIfChanged(book);

        assertFalse(indexed);
        verify(geminiClient, never()).generateEmbedding(anyString());
        verify(qdrantVectorService, never()).upsertPoints(anyList());
    }

    @Test
    @DisplayName("G. Changed book: Different contentHash in Qdrant triggers Gemini embedding and Qdrant upsert")
    void testChangedBookReindexed() {
        Book book = createSampleBook(30L, "Design Patterns", "GoF", "Technology", "Elements of Reusable Object-Oriented Software.");
        String oldHash = "stale_hash_from_old_description_12345";

        PointStruct existingPoint = PointStruct.of(30L, createMockVector(768), Map.of("contentHash", oldHash));
        when(qdrantVectorService.getPoint(30L)).thenReturn(existingPoint);
        when(geminiClient.generateEmbedding(anyString())).thenReturn(createMockVector(768));
        when(qdrantVectorService.upsertPoints(anyList())).thenReturn(true);

        boolean indexed = indexingService.indexBookIfChanged(book);

        assertTrue(indexed);
        verify(geminiClient, times(1)).generateEmbedding(anyString());
        verify(qdrantVectorService, times(1)).upsertPoints(anyList());
    }

    @Test
    @DisplayName("H. Delete: Deleting book vector forwards call to QdrantVectorService")
    void testDeleteBookVector() {
        when(qdrantVectorService.deletePoint(40L)).thenReturn(true);

        boolean deleted = indexingService.deleteBookVector(40L);

        assertTrue(deleted);
        verify(qdrantVectorService, times(1)).deletePoint(40L);
    }

    @Test
    @DisplayName("I. Batch indexing: Indexes new/changed, skips unchanged, and handles failure without aborting batch")
    void testIndexAllBooksBatch() {
        Book book1 = createSampleBook(1L, "Book 1", "Author 1", "Tech", "Desc 1");
        Book book2 = createSampleBook(2L, "Book 2", "Author 2", "Tech", "Desc 2");
        Book book3 = createSampleBook(3L, "Book 3", "Author 3", "Tech", "Desc 3");

        when(bookRepository.findAll()).thenReturn(List.of(book1, book2, book3));
        when(qdrantVectorService.ensureCollection()).thenReturn(true);

        // Book 1: Missing -> index successfully
        when(qdrantVectorService.getPoint(1L)).thenReturn(null);
        when(geminiClient.generateEmbedding(contains("Book 1"))).thenReturn(createMockVector(768));
        when(qdrantVectorService.upsertPoints(anyList())).thenReturn(true);

        // Book 2: Unchanged -> skip
        String book2Hash = indexingService.calculateContentHash(book2);
        when(qdrantVectorService.getPoint(2L)).thenReturn(
                PointStruct.of(2L, createMockVector(768), Map.of("contentHash", book2Hash))
        );

        // Book 3: Gemini throws exception -> fail this book, but don't abort batch
        when(qdrantVectorService.getPoint(3L)).thenReturn(null);
        when(geminiClient.generateEmbedding(contains("Book 3"))).thenThrow(new RuntimeException("Gemini quota exceeded"));

        IndexingResult result = indexingService.indexAllBooks();

        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getIndexed());
        assertEquals(1, result.getSkipped());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).contains("Book ID 3"));
    }
}
