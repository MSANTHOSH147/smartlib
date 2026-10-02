package com.smartlib.ai.service;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.entity.Book;
import com.smartlib.entity.Borrowing;
import com.smartlib.entity.User;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PersonalizedRecommendationService {

    private static final int DEFAULT_CANDIDATE_POOL = 15;

    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;
    private final GeminiClient geminiClient;
    private final QdrantVectorService qdrantVectorService;
    private final GeminiAiProperties geminiAiProperties;
    private final UserMemoryService userMemoryService;

    @org.springframework.beans.factory.annotation.Autowired
    public PersonalizedRecommendationService(BorrowingRepository borrowingRepository,
                                             BookRepository bookRepository,
                                             GeminiClient geminiClient,
                                             QdrantVectorService qdrantVectorService,
                                             GeminiAiProperties geminiAiProperties,
                                             UserMemoryService userMemoryService) {
        this.borrowingRepository = borrowingRepository;
        this.bookRepository = bookRepository;
        this.geminiClient = geminiClient;
        this.qdrantVectorService = qdrantVectorService;
        this.geminiAiProperties = geminiAiProperties;
        this.userMemoryService = userMemoryService;
    }

    public PersonalizedRecommendationService(BorrowingRepository borrowingRepository,
                                             BookRepository bookRepository,
                                             GeminiClient geminiClient,
                                             QdrantVectorService qdrantVectorService,
                                             GeminiAiProperties geminiAiProperties) {
        this(borrowingRepository, bookRepository, geminiClient, qdrantVectorService, geminiAiProperties, null);
    }

    public List<PersonalizedRecommendation> getRecommendations(User member) {
        return getRecommendations(member, 5);
    }

    public List<PersonalizedRecommendation> getRecommendations(User member, int limit) {
        if (member == null || member.getId() == null) {
            log.warn("Cannot provide recommendations for null user context.");
            return Collections.emptyList();
        }
        return getRecommendationsForUser(member.getId(), limit);
    }

    public List<PersonalizedRecommendation> getRecommendationsForUser(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));

        // 1. Retrieve borrowing history
        List<Borrowing> borrowings = borrowingRepository.findByUserId(userId);
        Set<Long> alreadyBorrowedBookIds = borrowings.stream()
                .filter(b -> b.getBook() != null && b.getBook().getId() != null)
                .map(b -> b.getBook().getId())
                .collect(Collectors.toSet());

        // Retrieve active memories for this member if memory service is available
        List<com.smartlib.ai.dto.UserMemoryDto> userMemories = Collections.emptyList();
        if (userMemoryService != null && userId != null) {
            try {
                userMemories = userMemoryService.getActiveMemories(User.builder().id(userId).build());
            } catch (Exception ex) {
                log.warn("Could not retrieve user memories for recommendations: {}", ex.getMessage());
            }
        }

        // 2. If member has no borrowing history, fall back to top-rated available books
        if (borrowings.isEmpty()) {
            log.debug("Member id={} has no borrowing history. Using generic library recommendations.", userId);
            return getGenericPopularRecommendations(alreadyBorrowedBookIds, safeLimit);
        }

        // 3. Build interest profile text from circulation history + memories
        String profileText = buildMemberProfileText(borrowings, userMemories);

        // 4. Retrieve candidate points from Qdrant via Gemini embedding
        List<ScoredPoint> candidates = retrieveSemanticCandidates(profileText);

        if (candidates.isEmpty()) {
            log.debug("No semantic candidates found or AI service unavailable. Falling back to category-based recommendation.");
            return getCategoryFallbackRecommendations(borrowings, alreadyBorrowedBookIds, safeLimit, userMemories);
        }

        // 5. Rank and hydrate candidate books
        return rankAndHydrateCandidates(candidates, alreadyBorrowedBookIds, safeLimit, userMemories);
    }

    private String buildMemberProfileText(List<Borrowing> borrowings, List<com.smartlib.ai.dto.UserMemoryDto> memories) {
        StringBuilder sb = new StringBuilder(buildMemberProfileText(borrowings));
        if (memories != null && !memories.isEmpty()) {
            for (com.smartlib.ai.dto.UserMemoryDto m : memories) {
                if (m.getValue() != null && !m.getValue().isBlank()) {
                    sb.append(" ").append(m.getValue());
                }
            }
        }
        return sb.toString().trim();
    }

    private String buildMemberProfileText(List<Borrowing> borrowings) {
        Map<String, Long> categoryCounts = borrowings.stream()
                .filter(b -> b.getBook() != null && b.getBook().getCategory() != null)
                .map(b -> b.getBook().getCategory().getName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        List<String> topCategories = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        List<String> authors = borrowings.stream()
                .filter(b -> b.getBook() != null && b.getBook().getAuthor() != null)
                .map(b -> b.getBook().getAuthor().trim())
                .distinct()
                .limit(4)
                .toList();

        List<String> recentTitles = borrowings.stream()
                .filter(b -> b.getBook() != null && b.getBook().getTitle() != null)
                .map(b -> b.getBook().getTitle().trim())
                .distinct()
                .limit(4)
                .toList();

        StringBuilder sb = new StringBuilder();
        if (!topCategories.isEmpty()) {
            sb.append("Preferred Genres: ").append(String.join(", ", topCategories)).append(". ");
        }
        if (!authors.isEmpty()) {
            sb.append("Favorite Authors: ").append(String.join(", ", authors)).append(". ");
        }
        if (!recentTitles.isEmpty()) {
            sb.append("Recently Enjoyed Books: ").append(String.join("; ", recentTitles)).append(".");
        }

        return sb.toString();
    }

    private List<ScoredPoint> retrieveSemanticCandidates(String profileText) {
        if (!geminiClient.isAvailable()) {
            log.debug("Gemini not configured; bypassing vector candidate retrieval.");
            return Collections.emptyList();
        }

        try {
            List<Float> vector = geminiClient.generateEmbedding(profileText);
            int expectedDim = geminiAiProperties.getEmbeddingDimension();

            if (vector == null || vector.size() != expectedDim) {
                log.warn("Profile query embedding dimension mismatch (expected {}, got {}).",
                        expectedDim, vector != null ? vector.size() : 0);
                return Collections.emptyList();
            }

            return qdrantVectorService.search(vector, DEFAULT_CANDIDATE_POOL);
        } catch (Exception ex) {
            log.warn("Failed to retrieve recommendation candidates from Qdrant: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PersonalizedRecommendation> rankAndHydrateCandidates(
            List<ScoredPoint> candidates,
            Set<Long> alreadyBorrowedBookIds,
            int limit) {
        return rankAndHydrateCandidates(candidates, alreadyBorrowedBookIds, limit, Collections.emptyList());
    }

    private List<PersonalizedRecommendation> rankAndHydrateCandidates(
            List<ScoredPoint> candidates,
            Set<Long> alreadyBorrowedBookIds,
            int limit,
            List<com.smartlib.ai.dto.UserMemoryDto> userMemories) {

        // Filter out already borrowed books and resolve IDs
        Map<Long, Double> candidateSimilarityMap = new LinkedHashMap<>();
        for (ScoredPoint point : candidates) {
            Long bookId = resolveBookId(point);
            if (bookId == null) continue;

            if (alreadyBorrowedBookIds.contains(bookId)) {
                log.debug("Excluding already borrowed book id={} from recommendations.", bookId);
                continue;
            }

            candidateSimilarityMap.putIfAbsent(bookId, point.getScore() != null ? point.getScore() : 0.0);
        }

        if (candidateSimilarityMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Hydrate from MySQL
        List<Book> books = bookRepository.findAllById(candidateSimilarityMap.keySet());
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        List<PersonalizedRecommendation> results = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : candidateSimilarityMap.entrySet()) {
            Long bookId = entry.getKey();
            Book book = bookMap.get(bookId);
            if (book == null) {
                // Stale Qdrant ID, safely skip
                continue;
            }

            double similarity = Math.max(0.0, Math.min(1.0, entry.getValue()));
            double rating = book.getAverageRating() != null ? Math.max(0.0, Math.min(1.0, book.getAverageRating() / 5.0)) : 0.0;
            boolean isAvailable = book.getAvailableCopies() != null && book.getAvailableCopies() > 0;
            double availabilityBoost = isAvailable ? 0.2 : 0.0;

            // Deterministic base formula: 60% similarity + 20% rating + 20% availability
            double baseScore = (similarity * 0.6) + (rating * 0.2) + availabilityBoost;

            List<String> reasonSignals = new ArrayList<>();
            if (book.getCategory() != null && book.getCategory().getName() != null) {
                reasonSignals.add("Matches your interest in " + book.getCategory().getName());
            }
            if (book.getAverageRating() != null && book.getAverageRating() >= 4.0) {
                reasonSignals.add("Community favorite (" + String.format("%.1f", book.getAverageRating()) + " ★)");
            }
            reasonSignals.add(isAvailable ? "Available to borrow now" : "Currently on loan");

            // Additional bounded memory signal: 85% base formula + 15% memory preference score
            double totalScore;
            if (userMemories != null && !userMemories.isEmpty()) {
                double memoryScore = computeMemoryScore(book, userMemories, reasonSignals);
                totalScore = (baseScore * 0.85) + (memoryScore * 0.15);
            } else {
                totalScore = baseScore;
            }

            results.add(PersonalizedRecommendation.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .categoryName(book.getCategory() != null ? book.getCategory().getName() : "")
                    .coverImageUrl(book.getCoverImageUrl())
                    .description(book.getDescription())
                    .averageRating(book.getAverageRating())
                    .availableCopies(book.getAvailableCopies())
                    .relevanceScore(Math.round(totalScore * 1000.0) / 1000.0)
                    .available(isAvailable)
                    .reasonSignals(reasonSignals)
                    .build());
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(PersonalizedRecommendation::getRelevanceScore).reversed())
                .limit(limit)
                .toList();
    }

    private double computeMemoryScore(Book book, List<com.smartlib.ai.dto.UserMemoryDto> memories, List<String> reasonSignals) {
        if (book == null || memories == null || memories.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String catName = (book.getCategory() != null && book.getCategory().getName() != null)
                ? book.getCategory().getName().toLowerCase(Locale.ROOT) : "";
        String author = (book.getAuthor() != null) ? book.getAuthor().toLowerCase(Locale.ROOT) : "";
        String title = (book.getTitle() != null) ? book.getTitle().toLowerCase(Locale.ROOT) : "";
        String desc = (book.getDescription() != null) ? book.getDescription().toLowerCase(Locale.ROOT) : "";

        for (com.smartlib.ai.dto.UserMemoryDto mem : memories) {
            String val = mem.getValue() != null ? mem.getValue().toLowerCase(Locale.ROOT) : "";
            if (val.isBlank()) continue;

            if (mem.getMemoryType() == com.smartlib.enums.MemoryType.CATEGORY
                    || mem.getMemoryType() == com.smartlib.enums.MemoryType.INTEREST
                    || mem.getMemoryType() == com.smartlib.enums.MemoryType.TOPIC) {
                if (!catName.isEmpty() && (catName.contains(val) || val.contains(catName))) {
                    score = Math.max(score, 0.8);
                    reasonSignals.add("Aligns with your remembered interest in " + mem.getValue());
                } else if (title.contains(val) || desc.contains(val)) {
                    score = Math.max(score, 0.6);
                    reasonSignals.add("Matches your remembered interest in " + mem.getValue());
                }
            } else if (mem.getMemoryType() == com.smartlib.enums.MemoryType.AUTHOR) {
                if (!author.isEmpty() && (author.contains(val) || val.contains(author))) {
                    score = Math.max(score, 1.0);
                    reasonSignals.add("By your favorite author " + mem.getValue());
                }
            } else if (mem.getMemoryType() == com.smartlib.enums.MemoryType.STYLE
                    || mem.getMemoryType() == com.smartlib.enums.MemoryType.PREFERENCE) {
                if (desc.contains(val) || title.contains(val)) {
                    score = Math.max(score, 0.5);
                    reasonSignals.add("Matches your reading preference (" + mem.getValue() + ")");
                }
            }
        }

        return Math.min(1.0, score);
    }

    private List<PersonalizedRecommendation> getGenericPopularRecommendations(Set<Long> alreadyBorrowedBookIds, int limit) {
        List<Book> availableBooks = bookRepository.findByAvailableCopiesGreaterThan(0);

        return availableBooks.stream()
                .filter(b -> !alreadyBorrowedBookIds.contains(b.getId()))
                .sorted(Comparator.comparingDouble((Book b) -> b.getAverageRating() != null ? b.getAverageRating() : 0.0).reversed())
                .limit(limit)
                .map(b -> PersonalizedRecommendation.builder()
                        .bookId(b.getId())
                        .title(b.getTitle())
                        .author(b.getAuthor())
                        .categoryName(b.getCategory() != null ? b.getCategory().getName() : "")
                        .coverImageUrl(b.getCoverImageUrl())
                        .description(b.getDescription())
                        .averageRating(b.getAverageRating())
                        .availableCopies(b.getAvailableCopies())
                        .relevanceScore(b.getAverageRating() != null ? b.getAverageRating() / 5.0 : 0.5)
                        .available(true)
                        .reasonSignals(List.of("Popular library recommendation", "Available to borrow now"))
                        .build())
                .toList();
    }

    private List<PersonalizedRecommendation> getCategoryFallbackRecommendations(
            List<Borrowing> borrowings,
            Set<Long> alreadyBorrowedBookIds,
            int limit) {
        return getCategoryFallbackRecommendations(borrowings, alreadyBorrowedBookIds, limit, Collections.emptyList());
    }

    private List<PersonalizedRecommendation> getCategoryFallbackRecommendations(
            List<Borrowing> borrowings,
            Set<Long> alreadyBorrowedBookIds,
            int limit,
            List<com.smartlib.ai.dto.UserMemoryDto> userMemories) {

        // Find most frequent category ID
        Map<Long, Long> categoryIdCounts = borrowings.stream()
                .filter(b -> b.getBook() != null && b.getBook().getCategory() != null)
                .map(b -> b.getBook().getCategory().getId())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        Optional<Long> topCategoryId = categoryIdCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (topCategoryId.isPresent()) {
            List<Book> categoryBooks = bookRepository.findByCategoryId(topCategoryId.get());
            List<PersonalizedRecommendation> recommendations = categoryBooks.stream()
                    .filter(b -> !alreadyBorrowedBookIds.contains(b.getId()))
                    .sorted(Comparator.comparingDouble((Book b) -> b.getAverageRating() != null ? b.getAverageRating() : 0.0).reversed())
                    .limit(limit)
                    .map(b -> {
                        List<String> reasonSignals = new ArrayList<>();
                        reasonSignals.add("Recommended based on your reading in " + (b.getCategory() != null ? b.getCategory().getName() : "this genre"));
                        double baseScore = b.getAverageRating() != null ? b.getAverageRating() / 5.0 : 0.5;
                        double totalScore = baseScore;
                        if (userMemories != null && !userMemories.isEmpty()) {
                            double memScore = computeMemoryScore(b, userMemories, reasonSignals);
                            totalScore = (baseScore * 0.85) + (memScore * 0.15);
                        }
                        return PersonalizedRecommendation.builder()
                                .bookId(b.getId())
                                .title(b.getTitle())
                                .author(b.getAuthor())
                                .categoryName(b.getCategory() != null ? b.getCategory().getName() : "")
                                .coverImageUrl(b.getCoverImageUrl())
                                .description(b.getDescription())
                                .averageRating(b.getAverageRating())
                                .availableCopies(b.getAvailableCopies())
                                .relevanceScore(Math.round(totalScore * 1000.0) / 1000.0)
                                .available(b.getAvailableCopies() != null && b.getAvailableCopies() > 0)
                                .reasonSignals(reasonSignals)
                                .build();
                    })
                    .toList();

            if (!recommendations.isEmpty()) {
                return recommendations;
            }
        }

        return getGenericPopularRecommendations(alreadyBorrowedBookIds, limit);
    }

    private Long resolveBookId(ScoredPoint point) {
        if (point.getPayload() != null && point.getPayload().get("bookId") != null) {
            Object raw = point.getPayload().get("bookId");
            if (raw instanceof Number num) return num.longValue();
            try {
                return Long.parseLong(raw.toString());
            } catch (NumberFormatException ignored) {}
        }
        if (point.getId() instanceof Number num) return num.longValue();
        try {
            return Long.parseLong(point.getId().toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
