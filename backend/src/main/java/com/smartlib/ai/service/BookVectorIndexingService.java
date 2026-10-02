package com.smartlib.ai.service;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.IndexingResult;
import com.smartlib.ai.dto.qdrant.PointStruct;
import com.smartlib.entity.Book;
import com.smartlib.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookVectorIndexingService {

    private final BookRepository bookRepository;
    private final GeminiClient geminiClient;
    private final QdrantVectorService qdrantVectorService;
    private final GeminiAiProperties geminiAiProperties;

    public String buildSearchableText(Book book) {
        if (book == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        appendField(sb, "Title", book.getTitle());
        appendField(sb, "Author", book.getAuthor());

        if (book.getCategory() != null && book.getCategory().getName() != null) {
            appendField(sb, "Category", book.getCategory().getName());
        }

        if (book.getPublisher() != null && !book.getPublisher().isBlank()) {
            appendField(sb, "Publisher", book.getPublisher());
        }

        if (book.getPublicationYear() != null) {
            appendField(sb, "Publication Year", String.valueOf(book.getPublicationYear()));
        }

        if (book.getIsbn() != null && !book.getIsbn().isBlank()) {
            appendField(sb, "ISBN", book.getIsbn());
        }

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            appendField(sb, "Description", book.getDescription().trim());
        }

        return sb.toString().trim();
    }

    private void appendField(StringBuilder sb, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(fieldName).append(": ").append(value.trim()).append("\n");
        }
    }

    public String calculateContentHash(Book book) {
        if (book == null) {
            return "";
        }

        String rawContent = String.join("|",
                normalize(book.getTitle()),
                normalize(book.getAuthor()),
                normalize(book.getCategory() != null ? book.getCategory().getName() : ""),
                normalize(book.getPublisher()),
                book.getPublicationYear() != null ? String.valueOf(book.getPublicationYear()) : "",
                normalize(book.getIsbn()),
                normalize(book.getDescription())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String normalize(String str) {
        return str != null ? str.trim().toLowerCase() : "";
    }

    public void indexBook(Book book) {
        if (book == null || book.getId() == null) {
            throw new IllegalArgumentException("Cannot index null book or book without ID");
        }

        log.info("Indexing book id={}", book.getId());

        String searchableText = buildSearchableText(book);
        if (searchableText.isBlank()) {
            throw new IllegalArgumentException("Searchable text is empty for book id=" + book.getId());
        }

        List<Float> vector = geminiClient.generateEmbedding(searchableText);
        int expectedDimension = geminiAiProperties.getEmbeddingDimension();

        if (vector == null || vector.isEmpty()) {
            throw new IllegalStateException("Gemini returned empty embedding for book id=" + book.getId());
        }

        if (vector.size() != expectedDimension) {
            log.error("Dimension mismatch for book id={}: expected {}, got {}",
                    book.getId(), expectedDimension, vector.size());
            throw new IllegalStateException("Embedding dimension mismatch: expected "
                    + expectedDimension + ", got " + vector.size());
        }

        String contentHash = calculateContentHash(book);

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookId", book.getId());
        payload.put("title", book.getTitle() != null ? book.getTitle() : "");
        payload.put("author", book.getAuthor() != null ? book.getAuthor() : "");
        if (book.getCategory() != null) {
            payload.put("categoryId", book.getCategory().getId());
            payload.put("categoryName", book.getCategory().getName() != null ? book.getCategory().getName() : "");
        }
        payload.put("contentHash", contentHash);
        payload.put("embeddingModel", geminiAiProperties.getEmbeddingModel());
        payload.put("embeddingDimension", expectedDimension);
        payload.put("indexedAt", Instant.now().toString());

        PointStruct point = PointStruct.of(book.getId(), vector, payload);
        boolean success = qdrantVectorService.upsertPoints(List.of(point));

        if (!success) {
            throw new RuntimeException("Failed to upsert vector into Qdrant for book id=" + book.getId());
        }

        log.info("Indexed book id={}", book.getId());
    }

    public boolean indexBookIfChanged(Book book) {
        if (book == null || book.getId() == null) {
            return false;
        }

        String currentHash = calculateContentHash(book);
        PointStruct existingPoint = qdrantVectorService.getPoint(book.getId());

        if (existingPoint != null && existingPoint.getPayload() != null) {
            Object storedHash = existingPoint.getPayload().get("contentHash");
            if (storedHash != null && currentHash.equals(storedHash.toString())) {
                log.debug("Skipping unchanged book id={}", book.getId());
                return false;
            }
        }

        indexBook(book);
        return true;
    }

    public boolean deleteBookVector(Long bookId) {
        if (bookId == null) {
            return false;
        }
        log.info("Deleting vector for book id={}", bookId);
        return qdrantVectorService.deletePoint(bookId);
    }

    public IndexingResult indexAllBooks() {
        log.info("Starting catalogue vector indexing sync...");
        qdrantVectorService.ensureCollection();

        List<Book> books = bookRepository.findAll();
        IndexingResult result = IndexingResult.builder()
                .total(books.size())
                .build();

        for (Book book : books) {
            try {
                boolean indexed = indexBookIfChanged(book);
                if (indexed) {
                    result.incrementIndexed();
                } else {
                    result.incrementSkipped();
                }
            } catch (Exception ex) {
                log.error("Failed to index book id={}: {}", book.getId(), ex.getMessage());
                result.incrementFailed(book.getId(), ex.getMessage());
            }
        }

        log.info("Catalogue vector indexing complete: total={}, indexed={}, skipped={}, failed={}",
                result.getTotal(), result.getIndexed(), result.getSkipped(), result.getFailed());

        return result;
    }
}
