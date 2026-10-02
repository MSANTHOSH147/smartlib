package com.smartlib.ai.service;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.entity.Book;
import com.smartlib.repository.BookRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HybridBookSearchService {

    private static final double RRF_K = 60.0;
    private static final int DEFAULT_CANDIDATE_LIMIT = 20;

    private final BookRepository bookRepository;
    private final GeminiClient geminiClient;
    private final QdrantVectorService qdrantVectorService;
    private final GeminiAiProperties geminiAiProperties;
    private final BookReranker bookReranker;

    @org.springframework.beans.factory.annotation.Autowired
    public HybridBookSearchService(BookRepository bookRepository,
                                  GeminiClient geminiClient,
                                  QdrantVectorService qdrantVectorService,
                                  GeminiAiProperties geminiAiProperties,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) BookReranker bookReranker) {
        this.bookRepository = bookRepository;
        this.geminiClient = geminiClient;
        this.qdrantVectorService = qdrantVectorService;
        this.geminiAiProperties = geminiAiProperties;
        this.bookReranker = bookReranker != null ? bookReranker : new DeterministicBookReranker();
    }

    public HybridBookSearchService(BookRepository bookRepository,
                                  GeminiClient geminiClient,
                                  QdrantVectorService qdrantVectorService,
                                  GeminiAiProperties geminiAiProperties) {
        this(bookRepository, geminiClient, qdrantVectorService, geminiAiProperties, new DeterministicBookReranker());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CandidateScore {
        private Long bookId;
        private Integer lexicalRank;
        private Integer semanticRank;
        private Double lexicalScore;
        private Double semanticScore;
        private double fusedScore;

        public void addRrfScore(double score) {
            this.fusedScore += score;
        }
    }

    public List<BookSearchResult> search(String query) {
        return search(query, 10);
    }

    public List<BookSearchResult> search(String query, int limit) {
        if (query == null || query.trim().isBlank()) {
            return Collections.emptyList();
        }

        String cleanQuery = query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 50));

        // 1. MySQL Lexical Branch
        List<Book> lexicalMatches = executeLexicalSearch(cleanQuery);

        // 2. Qdrant Semantic Branch (with graceful fallback)
        List<ScoredPoint> semanticMatches = executeSemanticSearch(cleanQuery);

        // 3. Reciprocal Rank Fusion (RRF)
        Map<Long, CandidateScore> candidateMap = performRrfFusion(lexicalMatches, semanticMatches);

        if (candidateMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Retrieve Top 20 Candidates from RRF
        int candidatePoolSize = Math.max(DEFAULT_CANDIDATE_LIMIT, safeLimit);
        List<CandidateScore> sortedCandidates = candidateMap.values().stream()
                .sorted(Comparator.comparingDouble(CandidateScore::getFusedScore).reversed())
                .limit(candidatePoolSize)
                .toList();

        // 5. Live MySQL Hydration of Top Candidates
        List<BookSearchResult> hydratedCandidates = hydrateResults(sortedCandidates);

        // 6. Deterministic Re-ranking Stage to Top Final Results
        if (bookReranker != null) {
            return bookReranker.rerank(cleanQuery, hydratedCandidates, safeLimit);
        }

        return hydratedCandidates.stream().limit(safeLimit).toList();
    }

    private List<Book> executeLexicalSearch(String query) {
        try {
            List<Book> results = bookRepository.searchLexical(query);
            if (results != null && results.size() > DEFAULT_CANDIDATE_LIMIT) {
                return results.subList(0, DEFAULT_CANDIDATE_LIMIT);
            }
            return results != null ? results : Collections.emptyList();
        } catch (Exception ex) {
            log.error("Error executing lexical search for query '{}': {}", query, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ScoredPoint> executeSemanticSearch(String query) {
        if (!geminiClient.isAvailable()) {
            log.debug("GeminiClient not configured; bypassing semantic branch.");
            return Collections.emptyList();
        }

        try {
            List<Float> vector = geminiClient.generateEmbedding(query);
            int expectedDim = geminiAiProperties.getEmbeddingDimension();

            if (vector == null || vector.size() != expectedDim) {
                log.warn("Invalid query embedding dimension (expected {}, got {}). Bypassing semantic search.",
                        expectedDim, vector != null ? vector.size() : 0);
                return Collections.emptyList();
            }

            return qdrantVectorService.search(vector, DEFAULT_CANDIDATE_LIMIT);
        } catch (Exception ex) {
            log.warn("Semantic search failed for query '{}': {}. Falling back to lexical results.",
                    query, ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<Long, CandidateScore> performRrfFusion(List<Book> lexicalMatches, List<ScoredPoint> semanticMatches) {
        Map<Long, CandidateScore> candidateMap = new LinkedHashMap<>();

        // Process Lexical Branch
        if (lexicalMatches != null) {
            for (int i = 0; i < lexicalMatches.size(); i++) {
                Book book = lexicalMatches.get(i);
                if (book == null || book.getId() == null) continue;

                int rank = i + 1; // 1-based rank
                double rrfScore = 1.0 / (RRF_K + rank);

                CandidateScore candidate = candidateMap.computeIfAbsent(book.getId(), id ->
                        CandidateScore.builder()
                                .bookId(id)
                                .build()
                );
                candidate.setLexicalRank(rank);
                candidate.setLexicalScore(rrfScore);
                candidate.addRrfScore(rrfScore);
            }
        }

        // Process Semantic Branch
        if (semanticMatches != null) {
            for (int i = 0; i < semanticMatches.size(); i++) {
                ScoredPoint point = semanticMatches.get(i);
                if (point == null) continue;

                Long bookId = resolveBookId(point);
                if (bookId == null) continue;

                int rank = i + 1; // 1-based rank
                double rrfScore = 1.0 / (RRF_K + rank);

                CandidateScore candidate = candidateMap.computeIfAbsent(bookId, id ->
                        CandidateScore.builder()
                                .bookId(id)
                                .build()
                );
                candidate.setSemanticRank(rank);
                candidate.setSemanticScore(point.getScore());
                candidate.addRrfScore(rrfScore);
            }
        }

        return candidateMap;
    }

    private Long resolveBookId(ScoredPoint point) {
        if (point.getPayload() != null && point.getPayload().get("bookId") != null) {
            Object rawBookId = point.getPayload().get("bookId");
            if (rawBookId instanceof Number num) {
                return num.longValue();
            }
            try {
                return Long.parseLong(rawBookId.toString());
            } catch (NumberFormatException ignored) {}
        }

        if (point.getId() instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(point.getId().toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private List<BookSearchResult> hydrateResults(List<CandidateScore> sortedCandidates) {
        List<Long> bookIds = sortedCandidates.stream()
                .map(CandidateScore::getBookId)
                .toList();

        Map<Long, Book> bookEntityMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<BookSearchResult> results = new ArrayList<>();

        for (CandidateScore candidate : sortedCandidates) {
            Book book = bookEntityMap.get(candidate.getBookId());
            if (book == null) {
                // Stale point in Qdrant for a deleted book; skip gracefully
                log.debug("Book id={} returned by vector index not found in MySQL. Skipping.", candidate.getBookId());
                continue;
            }

            results.add(BookSearchResult.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .categoryName(book.getCategory() != null ? book.getCategory().getName() : "")
                    .isbn(book.getIsbn())
                    .publisher(book.getPublisher())
                    .publicationYear(book.getPublicationYear())
                    .description(book.getDescription())
                    .coverImageUrl(book.getCoverImageUrl())
                    .totalCopies(book.getTotalCopies())
                    .availableCopies(book.getAvailableCopies())
                    .averageRating(book.getAverageRating())
                    .relevanceScore(candidate.getFusedScore())
                    .semanticScore(candidate.getSemanticScore())
                    .lexicalRank(candidate.getLexicalRank())
                    .semanticRank(candidate.getSemanticRank())
                    .build());
        }

        return results;
    }
}
