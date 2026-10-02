package com.smartlib.ai.tools;

import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.HybridBookSearchService;
import com.smartlib.ai.service.PersonalizedRecommendationService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.entity.*;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SmartLibToolExecutor {

    private final HybridBookSearchService hybridBookSearchService;
    private final PersonalizedRecommendationService personalizedRecommendationService;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowingRepository borrowingRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final QdrantVectorService qdrantVectorService;
    private final com.smartlib.ai.service.UserMemoryService userMemoryService;

    @org.springframework.beans.factory.annotation.Autowired
    public SmartLibToolExecutor(HybridBookSearchService hybridBookSearchService,
                                PersonalizedRecommendationService personalizedRecommendationService,
                                BookRepository bookRepository,
                                BookCopyRepository bookCopyRepository,
                                BorrowingRepository borrowingRepository,
                                ReservationRepository reservationRepository,
                                FineRepository fineRepository,
                                UserRepository userRepository,
                                GeminiClient geminiClient,
                                QdrantVectorService qdrantVectorService,
                                com.smartlib.ai.service.UserMemoryService userMemoryService) {
        this.hybridBookSearchService = hybridBookSearchService;
        this.personalizedRecommendationService = personalizedRecommendationService;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowingRepository = borrowingRepository;
        this.reservationRepository = reservationRepository;
        this.fineRepository = fineRepository;
        this.userRepository = userRepository;
        this.geminiClient = geminiClient;
        this.qdrantVectorService = qdrantVectorService;
        this.userMemoryService = userMemoryService;
    }

    public SmartLibToolExecutor(HybridBookSearchService hybridBookSearchService,
                                PersonalizedRecommendationService personalizedRecommendationService,
                                BookRepository bookRepository,
                                BookCopyRepository bookCopyRepository,
                                BorrowingRepository borrowingRepository,
                                ReservationRepository reservationRepository,
                                FineRepository fineRepository,
                                UserRepository userRepository,
                                GeminiClient geminiClient,
                                QdrantVectorService qdrantVectorService) {
        this(hybridBookSearchService, personalizedRecommendationService, bookRepository,
                bookCopyRepository, borrowingRepository, reservationRepository, fineRepository,
                userRepository, geminiClient, qdrantVectorService, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> executeTool(String toolName, Map<String, Object> arguments) {
        if (toolName == null || toolName.isBlank()) {
            return Map.of("error", "Tool name cannot be empty.");
        }

        Map<String, Object> safeArgs = (arguments != null) ? arguments : Collections.emptyMap();
        log.info("Executing AI tool: {}", toolName);

        try {
            return switch (toolName) {
                case SmartLibToolDefinitions.TOOL_SEARCH_BOOKS -> executeSearchBooks(safeArgs);
                case SmartLibToolDefinitions.TOOL_SEMANTIC_SEARCH_BOOKS -> executeSemanticSearchBooks(safeArgs);
                case SmartLibToolDefinitions.TOOL_GET_BOOK_DETAILS -> executeGetBookDetails(safeArgs);
                case SmartLibToolDefinitions.TOOL_CHECK_BOOK_AVAILABILITY -> executeCheckBookAvailability(safeArgs);
                case SmartLibToolDefinitions.TOOL_GET_SIMILAR_BOOKS -> executeGetSimilarBooks(safeArgs);
                case SmartLibToolDefinitions.TOOL_GET_MY_BORROWINGS -> executeGetMyBorrowings();
                case SmartLibToolDefinitions.TOOL_GET_MY_OVERDUE_BOOKS -> executeGetMyOverdueBooks();
                case SmartLibToolDefinitions.TOOL_GET_MY_RESERVATIONS -> executeGetMyReservations();
                case SmartLibToolDefinitions.TOOL_GET_MY_FINES -> executeGetMyFines();
                case SmartLibToolDefinitions.TOOL_GET_PERSONALIZED_RECOMMENDATIONS -> executeGetPersonalizedRecommendations(safeArgs);
                case SmartLibToolDefinitions.TOOL_GET_MY_MEMORIES -> executeGetMyMemories();
                case SmartLibToolDefinitions.TOOL_REMEMBER_PREFERENCE -> executeRememberPreference(safeArgs);
                case SmartLibToolDefinitions.TOOL_FORGET_MY_MEMORY -> executeForgetMyMemory(safeArgs);
                default -> {
                    log.warn("Unknown tool requested: {}", toolName);
                    yield Map.of("error", "Unknown tool: " + toolName);
                }
            };
        } catch (Exception ex) {
            log.error("Error executing tool '{}': {}", toolName, ex.getMessage(), ex);
            return Map.of("error", "Failed to execute library tool: " + toolName + ". " + ex.getMessage());
        }
    }

    private Map<String, Object> executeSearchBooks(Map<String, Object> args) {
        String query = parseString(args, "query");
        if (query == null || query.isBlank()) {
            return Map.of("error", "The 'query' parameter is required for searchBooks.");
        }
        if (query.length() > 500) {
            query = query.substring(0, 500);
        }
        int limit = parseInt(args, "limit", 10, 1, 20);

        List<BookSearchResult> searchResults = hybridBookSearchService.search(query, limit);
        List<Map<String, Object>> compactResults = searchResults.stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookId", r.getBookId());
                    map.put("title", r.getTitle());
                    map.put("author", r.getAuthor());
                    map.put("category", r.getCategoryName());
                    map.put("availableCopies", r.getAvailableCopies());
                    map.put("totalCopies", r.getTotalCopies());
                    map.put("averageRating", r.getAverageRating());
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("results", compactResults);
        out.put("totalFound", compactResults.size());
        return out;
    }

    private Map<String, Object> executeSemanticSearchBooks(Map<String, Object> args) {
        String query = parseString(args, "query");
        if (query == null || query.isBlank()) {
            return Map.of("error", "The 'query' parameter is required for semanticSearchBooks.");
        }
        if (query.length() > 500) {
            query = query.substring(0, 500);
        }
        int limit = parseInt(args, "limit", 10, 1, 20);

        List<Float> vector = geminiClient.generateEmbedding(query);
        if (vector.isEmpty()) {
            return Map.of("results", Collections.emptyList(), "totalFound", 0);
        }

        List<ScoredPoint> points = qdrantVectorService.search(vector, limit);
        if (points.isEmpty()) {
            return Map.of("results", Collections.emptyList(), "totalFound", 0);
        }

        List<Long> bookIds = points.stream()
                .map(this::resolveBookId)
                .filter(Objects::nonNull)
                .toList();

        if (bookIds.isEmpty()) {
            return Map.of("results", Collections.emptyList(), "totalFound", 0);
        }

        Map<Long, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<Map<String, Object>> compactResults = new ArrayList<>();
        for (ScoredPoint pt : points) {
            Long bId = resolveBookId(pt);
            if (bId != null) {
                Book book = bookMap.get(bId);
                if (book != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookId", book.getId());
                    map.put("title", book.getTitle());
                    map.put("author", book.getAuthor());
                    map.put("category", book.getCategory() != null ? book.getCategory().getName() : null);
                    map.put("availableCopies", book.getAvailableCopies());
                    map.put("totalCopies", book.getTotalCopies());
                    map.put("similarityScore", pt.getScore());
                    compactResults.add(map);
                }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("results", compactResults);
        out.put("totalFound", compactResults.size());
        return out;
    }

    private Map<String, Object> executeGetBookDetails(Map<String, Object> args) {
        Long bookId = parseLong(args, "bookId");
        if (bookId == null || bookId <= 0) {
            return Map.of("error", "Valid numeric 'bookId' is required for getBookDetails.");
        }

        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return Map.of("error", "Book not found with ID: " + bookId);
        }

        Book book = bookOpt.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bookId", book.getId());
        out.put("title", book.getTitle());
        out.put("author", book.getAuthor());
        out.put("isbn", book.getIsbn());
        out.put("publisher", book.getPublisher());
        out.put("publicationYear", book.getPublicationYear());
        out.put("category", book.getCategory() != null ? book.getCategory().getName() : null);
        out.put("description", book.getDescription());
        out.put("availableCopies", book.getAvailableCopies());
        out.put("totalCopies", book.getTotalCopies());
        out.put("averageRating", book.getAverageRating());
        return out;
    }

    private Map<String, Object> executeCheckBookAvailability(Map<String, Object> args) {
        Long bookId = parseLong(args, "bookId");
        if (bookId == null || bookId <= 0) {
            return Map.of("error", "Valid numeric 'bookId' is required for checkBookAvailability.");
        }

        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return Map.of("error", "Book not found with ID: " + bookId);
        }

        Book book = bookOpt.get();
        List<BookCopy> copies = bookCopyRepository.findByBookId(bookId);

        List<Map<String, Object>> copyDetails = copies.stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("copyNumber", c.getCopyNumber());
                    map.put("status", c.getStatus() != null ? c.getStatus().name() : "UNKNOWN");
                    map.put("condition", c.getCondition() != null ? c.getCondition().name() : "UNKNOWN");
                    map.put("location", c.getLocation());
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bookId", book.getId());
        out.put("title", book.getTitle());
        out.put("availableCopies", book.getAvailableCopies());
        out.put("totalCopies", book.getTotalCopies());
        out.put("isAvailable", book.getAvailableCopies() != null && book.getAvailableCopies() > 0);
        out.put("copies", copyDetails);
        return out;
    }

    private Map<String, Object> executeGetSimilarBooks(Map<String, Object> args) {
        Long bookId = parseLong(args, "bookId");
        if (bookId == null || bookId <= 0) {
            return Map.of("error", "Valid numeric 'bookId' is required for getSimilarBooks.");
        }
        int limit = parseInt(args, "limit", 5, 1, 10);

        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            return Map.of("error", "Book not found with ID: " + bookId);
        }

        Book refBook = bookOpt.get();
        String text = String.format("Title: %s. Author: %s. Category: %s. %s",
                refBook.getTitle(),
                refBook.getAuthor(),
                refBook.getCategory() != null ? refBook.getCategory().getName() : "General",
                refBook.getDescription() != null ? refBook.getDescription() : "");

        List<Float> vector = geminiClient.generateEmbedding(text);
        if (vector.isEmpty()) {
            return Map.of("referenceBookId", bookId, "similarBooks", Collections.emptyList());
        }

        List<ScoredPoint> points = qdrantVectorService.search(vector, limit + 5);

        List<Long> similarIds = points.stream()
                .map(this::resolveBookId)
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, bookId))
                .limit(limit)
                .toList();

        if (similarIds.isEmpty()) {
            return Map.of("referenceBookId", bookId, "similarBooks", Collections.emptyList());
        }

        Map<Long, Book> bookMap = bookRepository.findAllById(similarIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));

        List<Map<String, Object>> similarBooks = new ArrayList<>();
        for (Long id : similarIds) {
            Book b = bookMap.get(id);
            if (b != null) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("bookId", b.getId());
                map.put("title", b.getTitle());
                map.put("author", b.getAuthor());
                map.put("category", b.getCategory() != null ? b.getCategory().getName() : null);
                map.put("availableCopies", b.getAvailableCopies());
                similarBooks.add(map);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("referenceBookId", bookId);
        out.put("similarBooks", similarBooks);
        return out;
    }

    private Map<String, Object> executeGetMyBorrowings() {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }

        List<Borrowing> borrowings = borrowingRepository.findByUserId(user.getId());
        List<Map<String, Object>> list = borrowings.stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("borrowingId", b.getId());
                    map.put("bookId", b.getBook() != null ? b.getBook().getId() : null);
                    map.put("title", b.getBook() != null ? b.getBook().getTitle() : null);
                    map.put("author", b.getBook() != null ? b.getBook().getAuthor() : null);
                    map.put("borrowDate", b.getBorrowDate() != null ? b.getBorrowDate().toString() : null);
                    map.put("dueDate", b.getDueDate() != null ? b.getDueDate().toString() : null);
                    map.put("returnDate", b.getReturnDate() != null ? b.getReturnDate().toString() : null);
                    map.put("status", b.getStatus() != null ? b.getStatus().name() : null);
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("borrowings", list);
        out.put("count", list.size());
        return out;
    }

    private Map<String, Object> executeGetMyOverdueBooks() {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }

        LocalDate now = LocalDate.now();
        List<Borrowing> borrowings = borrowingRepository.findByUserId(user.getId());
        List<Map<String, Object>> overdueList = borrowings.stream()
                .filter(b -> b.getStatus() == BorrowStatus.OVERDUE ||
                        (b.getStatus() == BorrowStatus.BORROWED && b.getDueDate() != null && b.getDueDate().isBefore(now)))
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("borrowingId", b.getId());
                    map.put("bookId", b.getBook() != null ? b.getBook().getId() : null);
                    map.put("title", b.getBook() != null ? b.getBook().getTitle() : null);
                    map.put("author", b.getBook() != null ? b.getBook().getAuthor() : null);
                    map.put("borrowDate", b.getBorrowDate() != null ? b.getBorrowDate().toString() : null);
                    map.put("dueDate", b.getDueDate() != null ? b.getDueDate().toString() : null);
                    long daysOverdue = b.getDueDate() != null && b.getDueDate().isBefore(now)
                            ? ChronoUnit.DAYS.between(b.getDueDate(), now)
                            : 0;
                    map.put("overdueDays", Math.max(0, daysOverdue));
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overdueBooks", overdueList);
        out.put("count", overdueList.size());
        return out;
    }

    private Map<String, Object> executeGetMyReservations() {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }

        List<Reservation> reservations = reservationRepository.findByUserId(user.getId());
        List<Map<String, Object>> list = reservations.stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("reservationId", r.getId());
                    map.put("bookId", r.getBook() != null ? r.getBook().getId() : null);
                    map.put("title", r.getBook() != null ? r.getBook().getTitle() : null);
                    map.put("author", r.getBook() != null ? r.getBook().getAuthor() : null);
                    map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
                    map.put("reservedAt", r.getReservedAt() != null ? r.getReservedAt().toString() : null);
                    map.put("readyAt", r.getReadyAt() != null ? r.getReadyAt().toString() : null);
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reservations", list);
        out.put("count", list.size());
        return out;
    }

    private Map<String, Object> executeGetMyFines() {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }

        List<Fine> fines = fineRepository.findByUserId(user.getId());
        double totalUnpaid = fines.stream()
                .filter(f -> f.getStatus() != null && "UNPAID".equalsIgnoreCase(f.getStatus().name()))
                .mapToDouble(f -> f.getAmount() != null ? f.getAmount() : 0.0)
                .sum();

        List<Map<String, Object>> list = fines.stream()
                .map(f -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("fineId", f.getId());
                    map.put("borrowingId", f.getBorrowing() != null ? f.getBorrowing().getId() : null);
                    map.put("bookTitle", (f.getBorrowing() != null && f.getBorrowing().getBook() != null)
                            ? f.getBorrowing().getBook().getTitle() : null);
                    map.put("amount", f.getAmount());
                    map.put("status", f.getStatus() != null ? f.getStatus().name() : null);
                    map.put("paidAt", f.getPaidAt() != null ? f.getPaidAt().toString() : null);
                    map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fines", list);
        out.put("totalUnpaidAmount", totalUnpaid);
        out.put("count", list.size());
        return out;
    }

    private Map<String, Object> executeGetPersonalizedRecommendations(Map<String, Object> args) {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }

        int limit = parseInt(args, "limit", 5, 1, 10);
        List<PersonalizedRecommendation> recs = personalizedRecommendationService.getRecommendations(user, limit);

        List<Map<String, Object>> list = recs.stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookId", r.getBookId());
                    map.put("title", r.getTitle());
                    map.put("author", r.getAuthor());
                    map.put("category", r.getCategoryName());
                    map.put("availableCopies", r.getAvailableCopies());
                    map.put("recommendationReason", r.getReasonSignals() != null && !r.getReasonSignals().isEmpty()
                            ? String.join("; ", r.getReasonSignals())
                            : "Recommended based on your library activity.");
                    return map;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recommendations", list);
        out.put("count", list.size());
        return out;
    }

    private Long resolveBookId(ScoredPoint point) {
        if (point == null) return null;
        if (point.getPayload() != null && point.getPayload().get("bookId") != null) {
            Object raw = point.getPayload().get("bookId");
            if (raw instanceof Number num) return num.longValue();
            try {
                return Long.parseLong(raw.toString());
            } catch (NumberFormatException ignored) {}
        }
        if (point.getId() instanceof Number num) return num.longValue();
        if (point.getId() != null) {
            try {
                return Long.parseLong(point.getId().toString());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private User resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (!auth.isAuthenticated()) {
            return null;
        }
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private Map<String, Object> unauthorizedError() {
        return Map.of("error", "Authentication required. Please sign in to access personal library information.");
    }

    private String parseString(Map<String, Object> args, String key) {
        Object val = args.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private Long parseLong(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(val.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> executeGetMyMemories() {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }
        if (userMemoryService == null) {
            return Map.of("memories", Collections.emptyList(), "count", 0);
        }

        List<com.smartlib.ai.dto.UserMemoryDto> memories = userMemoryService.getActiveMemories(user);
        List<Map<String, Object>> memoryList = memories.stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("type", m.getMemoryType() != null ? m.getMemoryType().name() : "PREFERENCE");
                    map.put("key", m.getKey());
                    map.put("value", m.getValue());
                    map.put("confidence", m.getConfidence());
                    return map;
                })
                .toList();

        return Map.of(
                "memories", memoryList,
                "count", memoryList.size()
        );
    }

    private Map<String, Object> executeRememberPreference(Map<String, Object> args) {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }
        if (userMemoryService == null) {
            return Map.of("error", "User memory service is not available.");
        }

        String typeStr = parseString(args, "type");
        String key = parseString(args, "key");
        String value = parseString(args, "value");

        if (typeStr == null || typeStr.isBlank()) {
            return Map.of("error", "Memory type is required (e.g. PREFERENCE, INTEREST, AUTHOR, TOPIC, CATEGORY, STYLE).");
        }
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return Map.of("error", "Both 'key' and 'value' parameters are required for rememberPreference.");
        }

        com.smartlib.enums.MemoryType memoryType;
        try {
            memoryType = com.smartlib.enums.MemoryType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Invalid memory type: " + typeStr + ". Allowed types: PREFERENCE, INTEREST, AUTHOR, TOPIC, CATEGORY, STYLE.");
        }

        Double confidence = 0.90;
        if (args.get("confidence") instanceof Number n) {
            confidence = n.doubleValue();
        }

        try {
            com.smartlib.ai.dto.UserMemoryDto saved = userMemoryService.saveOrUpdateMemory(
                    user,
                    memoryType,
                    key,
                    value,
                    confidence,
                    com.smartlib.enums.MemorySource.EXPLICIT_USER,
                    null
            );
            return Map.of(
                    "status", "SAVED",
                    "success", true,
                    "message", "Successfully remembered " + saved.getKey() + ": " + saved.getValue(),
                    "key", saved.getKey(),
                    "value", saved.getValue(),
                    "memoryId", saved.getId() != null ? saved.getId() : -1L
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to remember preference: {}", ex.getMessage());
            return Map.of("error", ex.getMessage());
        }
    }

    private Map<String, Object> executeForgetMyMemory(Map<String, Object> args) {
        User user = resolveAuthenticatedUser();
        if (user == null) {
            return unauthorizedError();
        }
        if (userMemoryService == null) {
            return Map.of("error", "User memory service is not available.");
        }

        Long memoryId = parseLong(args, "memoryId");
        String key = parseString(args, "key");

        if (memoryId == null && (key == null || key.isBlank())) {
            return Map.of("error", "Either 'key' or 'memoryId' must be provided to forget a memory.");
        }

        boolean deactivated;
        if (memoryId != null) {
            deactivated = userMemoryService.deactivateMemory(user, memoryId);
        } else {
            deactivated = userMemoryService.deactivateMemoryByKey(user, key);
        }

        if (deactivated) {
            return Map.of(
                    "status", "FORGOTTEN",
                    "success", true,
                    "message", "Successfully forgot the specified preference."
            );
        } else {
            return Map.of(
                    "status", "NOT_FOUND",
                    "success", false,
                    "message", "No matching active memory found to forget."
            );
        }
    }

    private int parseInt(Map<String, Object> args, String key, int defaultValue, int min, int max) {
        Object val = args.get(key);
        if (val == null) {
            return defaultValue;
        }
        if (val instanceof Number n) {
            return Math.max(min, Math.min(n.intValue(), max));
        }
        try {
            int parsed = Integer.parseInt(val.toString().trim());
            return Math.max(min, Math.min(parsed, max));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
