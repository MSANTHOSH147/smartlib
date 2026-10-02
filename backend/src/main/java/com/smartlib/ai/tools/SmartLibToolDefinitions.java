package com.smartlib.ai.tools;

import com.smartlib.ai.dto.gemini.FunctionDeclaration;
import com.smartlib.ai.dto.gemini.Tool;

import java.util.*;

public final class SmartLibToolDefinitions {

    private SmartLibToolDefinitions() {}

    public static final String TOOL_SEARCH_BOOKS = "searchBooks";
    public static final String TOOL_SEMANTIC_SEARCH_BOOKS = "semanticSearchBooks";
    public static final String TOOL_GET_BOOK_DETAILS = "getBookDetails";
    public static final String TOOL_CHECK_BOOK_AVAILABILITY = "checkBookAvailability";
    public static final String TOOL_GET_SIMILAR_BOOKS = "getSimilarBooks";
    public static final String TOOL_GET_MY_BORROWINGS = "getMyBorrowings";
    public static final String TOOL_GET_MY_OVERDUE_BOOKS = "getMyOverdueBooks";
    public static final String TOOL_GET_MY_RESERVATIONS = "getMyReservations";
    public static final String TOOL_GET_MY_FINES = "getMyFines";
    public static final String TOOL_GET_PERSONALIZED_RECOMMENDATIONS = "getPersonalizedRecommendations";
    public static final String TOOL_GET_MY_MEMORIES = "getMyMemories";
    public static final String TOOL_REMEMBER_PREFERENCE = "rememberPreference";
    public static final String TOOL_FORGET_MY_MEMORY = "forgetMyMemory";

    public static Tool getSmartLibTool() {
        return Tool.of(getAllDeclarations());
    }

    public static List<FunctionDeclaration> getAllDeclarations() {
        return List.of(
                createSearchBooksDeclaration(),
                createSemanticSearchBooksDeclaration(),
                createGetBookDetailsDeclaration(),
                createCheckBookAvailabilityDeclaration(),
                createGetSimilarBooksDeclaration(),
                createGetMyBorrowingsDeclaration(),
                createGetMyOverdueBooksDeclaration(),
                createGetMyReservationsDeclaration(),
                createGetMyFinesDeclaration(),
                createGetPersonalizedRecommendationsDeclaration(),
                createGetMyMemoriesDeclaration(),
                createRememberPreferenceDeclaration(),
                createForgetMyMemoryDeclaration()
        );
    }

    private static FunctionDeclaration createSearchBooksDeclaration() {
        Map<String, Object> queryProp = Map.of(
                "type", "STRING",
                "description", "Keyword, title, author, or topic to search in the SmartLib library catalog."
        );
        Map<String, Object> limitProp = Map.of(
                "type", "INTEGER",
                "description", "Maximum number of search results to return (default 10, max 20)."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("query", queryProp, "limit", limitProp),
                "required", List.of("query")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_SEARCH_BOOKS)
                .description("Search the SmartLib library catalog using hybrid lexical and semantic search. Use this for questions about books available in this library.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createSemanticSearchBooksDeclaration() {
        Map<String, Object> queryProp = Map.of(
                "type", "STRING",
                "description", "Natural language or conceptual query to search semantically."
        );
        Map<String, Object> limitProp = Map.of(
                "type", "INTEGER",
                "description", "Maximum number of results to return (default 10, max 20)."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("query", queryProp, "limit", limitProp),
                "required", List.of("query")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_SEMANTIC_SEARCH_BOOKS)
                .description("Perform a semantic search in the SmartLib book vector database. Useful for conceptual or topic-based book discovery.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetBookDetailsDeclaration() {
        Map<String, Object> bookIdProp = Map.of(
                "type", "INTEGER",
                "description", "The unique ID of the SmartLib book."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("bookId", bookIdProp),
                "required", List.of("bookId")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_BOOK_DETAILS)
                .description("Retrieve authoritative details for a specific book by ID, including title, author, category, ISBN, publisher, and community rating.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createCheckBookAvailabilityDeclaration() {
        Map<String, Object> bookIdProp = Map.of(
                "type", "INTEGER",
                "description", "The unique ID of the SmartLib book to check."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("bookId", bookIdProp),
                "required", List.of("bookId")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_CHECK_BOOK_AVAILABILITY)
                .description("Check the current physical copy availability, total copies, and shelf locations for a SmartLib book.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetSimilarBooksDeclaration() {
        Map<String, Object> bookIdProp = Map.of(
                "type", "INTEGER",
                "description", "The reference book ID to find similar books for."
        );
        Map<String, Object> limitProp = Map.of(
                "type", "INTEGER",
                "description", "Maximum number of similar books to return (default 5)."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("bookId", bookIdProp, "limit", limitProp),
                "required", List.of("bookId")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_SIMILAR_BOOKS)
                .description("Find books semantically similar to a specific book in SmartLib.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetMyBorrowingsDeclaration() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Collections.emptyMap()
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_MY_BORROWINGS)
                .description("Get current and past book borrowings for the currently authenticated member. Requires authenticated user context.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetMyOverdueBooksDeclaration() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Collections.emptyMap()
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_MY_OVERDUE_BOOKS)
                .description("Get overdue books for the currently authenticated member, along with due dates and overdue days.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetMyReservationsDeclaration() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Collections.emptyMap()
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_MY_RESERVATIONS)
                .description("Get active and pending book reservations for the currently authenticated member.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetMyFinesDeclaration() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Collections.emptyMap()
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_MY_FINES)
                .description("Get fines, unpaid balances, and payment statuses for the currently authenticated member.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetPersonalizedRecommendationsDeclaration() {
        Map<String, Object> limitProp = Map.of(
                "type", "INTEGER",
                "description", "Maximum number of recommendations to return (default 5, max 10)."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of("limit", limitProp)
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_PERSONALIZED_RECOMMENDATIONS)
                .description("Get personalized book recommendations for the currently authenticated member based on their reading history and remembered preferences.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createGetMyMemoriesDeclaration() {
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Collections.emptyMap()
        );
        return FunctionDeclaration.builder()
                .name(TOOL_GET_MY_MEMORIES)
                .description("Retrieve the active memories, preferences, favorite authors, and reading interests stored for the currently authenticated member.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createRememberPreferenceDeclaration() {
        Map<String, Object> typeProp = Map.of(
                "type", "STRING",
                "description", "Memory type: PREFERENCE, INTEREST, AUTHOR, TOPIC, CATEGORY, or STYLE."
        );
        Map<String, Object> keyProp = Map.of(
                "type", "STRING",
                "description", "Short identifier for the preference, e.g. 'preferred_category', 'favorite_author', 'reading_style'."
        );
        Map<String, Object> valProp = Map.of(
                "type", "STRING",
                "description", "The preference content, e.g. 'Java', 'Martin Fowler', 'Practical examples'."
        );
        Map<String, Object> confProp = Map.of(
                "type", "NUMBER",
                "description", "Optional confidence level between 0.0 and 1.0 (default 0.9)."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "type", typeProp,
                        "key", keyProp,
                        "value", valProp,
                        "confidence", confProp
                ),
                "required", List.of("type", "key", "value")
        );
        return FunctionDeclaration.builder()
                .name(TOOL_REMEMBER_PREFERENCE)
                .description("Remember or update a persistent user preference, interest, favorite author, or reading style for the currently authenticated member.")
                .parameters(params)
                .build();
    }

    private static FunctionDeclaration createForgetMyMemoryDeclaration() {
        Map<String, Object> keyProp = Map.of(
                "type", "STRING",
                "description", "The preference key, category, or topic to forget, e.g. 'preferred_category' or 'Java'."
        );
        Map<String, Object> idProp = Map.of(
                "type", "INTEGER",
                "description", "Optional database ID of the specific memory record to deactivate."
        );
        Map<String, Object> params = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "key", keyProp,
                        "memoryId", idProp
                )
        );
        return FunctionDeclaration.builder()
                .name(TOOL_FORGET_MY_MEMORY)
                .description("Forget or deactivate a stored memory, preference, or reading interest for the currently authenticated member.")
                .parameters(params)
                .build();
    }
}
