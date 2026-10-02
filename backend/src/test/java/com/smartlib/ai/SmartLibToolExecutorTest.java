package com.smartlib.ai;

import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.gemini.FunctionDeclaration;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.HybridBookSearchService;
import com.smartlib.ai.service.PersonalizedRecommendationService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.ai.tools.SmartLibToolDefinitions;
import com.smartlib.ai.tools.SmartLibToolExecutor;
import com.smartlib.entity.*;
import com.smartlib.enums.BookCopyCondition;
import com.smartlib.enums.BookCopyStatus;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.enums.FineStatus;
import com.smartlib.enums.ReservationStatus;
import com.smartlib.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartLibToolExecutorTest {

    @Mock
    private HybridBookSearchService hybridBookSearchService;

    @Mock
    private PersonalizedRecommendationService personalizedRecommendationService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private FineRepository fineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private SmartLibToolExecutor toolExecutor;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .name("Alice Test")
                .email("alice@test.com")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .isbn("978-0134494166")
                .category(Category.builder().id(10L).name("Software Engineering").build())
                .description("A craftsman's guide to software structure and design.")
                .availableCopies(2)
                .totalCopies(3)
                .averageRating(4.8)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // A. Tool registration
    @Test
    @DisplayName("A. Verify all 10 required tools are registered")
    void testAllToolsRegistered() {
        List<FunctionDeclaration> declarations = SmartLibToolDefinitions.getAllDeclarations();
        assertThat(declarations).hasSize(13);

        List<String> names = declarations.stream().map(FunctionDeclaration::getName).toList();
        assertThat(names).containsExactlyInAnyOrder(
                SmartLibToolDefinitions.TOOL_SEARCH_BOOKS,
                SmartLibToolDefinitions.TOOL_SEMANTIC_SEARCH_BOOKS,
                SmartLibToolDefinitions.TOOL_GET_BOOK_DETAILS,
                SmartLibToolDefinitions.TOOL_CHECK_BOOK_AVAILABILITY,
                SmartLibToolDefinitions.TOOL_GET_SIMILAR_BOOKS,
                SmartLibToolDefinitions.TOOL_GET_MY_BORROWINGS,
                SmartLibToolDefinitions.TOOL_GET_MY_OVERDUE_BOOKS,
                SmartLibToolDefinitions.TOOL_GET_MY_RESERVATIONS,
                SmartLibToolDefinitions.TOOL_GET_MY_FINES,
                SmartLibToolDefinitions.TOOL_GET_PERSONALIZED_RECOMMENDATIONS,
                SmartLibToolDefinitions.TOOL_GET_MY_MEMORIES,
                SmartLibToolDefinitions.TOOL_REMEMBER_PREFERENCE,
                SmartLibToolDefinitions.TOOL_FORGET_MY_MEMORY
        );
    }

    // B. Tool schema
    @Test
    @DisplayName("B. Verify tool schemas and required parameters are correctly represented")
    void testToolSchemaDefinitions() {
        List<FunctionDeclaration> declarations = SmartLibToolDefinitions.getAllDeclarations();
        Map<String, FunctionDeclaration> map = new HashMap<>();
        for (FunctionDeclaration fd : declarations) {
            map.put(fd.getName(), fd);
        }

        // searchBooks requires query
        FunctionDeclaration searchDecl = map.get(SmartLibToolDefinitions.TOOL_SEARCH_BOOKS);
        assertThat(searchDecl.getParameters()).containsKey("required");
        @SuppressWarnings("unchecked")
        List<String> searchReq = (List<String>) searchDecl.getParameters().get("required");
        assertThat(searchReq).contains("query");

        // getBookDetails requires bookId
        FunctionDeclaration detailsDecl = map.get(SmartLibToolDefinitions.TOOL_GET_BOOK_DETAILS);
        assertThat(detailsDecl.getParameters()).containsKey("required");
        @SuppressWarnings("unchecked")
        List<String> detailsReq = (List<String>) detailsDecl.getParameters().get("required");
        assertThat(detailsReq).contains("bookId");

        // getMyBorrowings has no required parameters
        FunctionDeclaration borrowDecl = map.get(SmartLibToolDefinitions.TOOL_GET_MY_BORROWINGS);
        assertThat(borrowDecl.getParameters().get("required")).isNull();
    }

    // C. searchBooks execution
    @Test
    @DisplayName("C. Verify searchBooks arguments reach HybridBookSearchService")
    void testSearchBooksExecution() {
        when(hybridBookSearchService.search("Clean Code", 5)).thenReturn(List.of(
                BookSearchResult.builder()
                        .bookId(1L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .categoryName("Programming")
                        .availableCopies(2)
                        .totalCopies(3)
                        .averageRating(4.9)
                        .build()
        ));

        Map<String, Object> result = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_SEARCH_BOOKS,
                Map.of("query", "Clean Code", "limit", 5)
        );

        verify(hybridBookSearchService).search("Clean Code", 5);
        assertThat(result).containsKey("results");
        assertThat(result.get("totalFound")).isEqualTo(1);
    }

    // D. getBookDetails execution
    @Test
    @DisplayName("D. Verify getBookDetails execution retrieves authoritative book data")
    void testGetBookDetailsExecution() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        Map<String, Object> result = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_GET_BOOK_DETAILS,
                Map.of("bookId", 1L)
        );

        verify(bookRepository).findById(1L);
        assertThat(result.get("title")).isEqualTo("Clean Architecture");
        assertThat(result.get("isbn")).isEqualTo("978-0134494166");
        assertThat(result.get("category")).isEqualTo("Software Engineering");
    }

    // E. checkBookAvailability execution
    @Test
    @DisplayName("E. Verify checkBookAvailability execution does not leak qrToken")
    void testCheckBookAvailabilityExecution() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        BookCopy copy1 = BookCopy.builder()
                .id(101L)
                .copyNumber("C-001")
                .status(BookCopyStatus.AVAILABLE)
                .condition(BookCopyCondition.GOOD)
                .location("Shelf A1")
                .qrToken("SECRET_QR_TOKEN_123")
                .build();

        when(bookCopyRepository.findByBookId(1L)).thenReturn(List.of(copy1));

        Map<String, Object> result = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_CHECK_BOOK_AVAILABILITY,
                Map.of("bookId", 1L)
        );

        assertThat(result.get("isAvailable")).isEqualTo(true);
        assertThat(result.get("availableCopies")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> copies = (List<Map<String, Object>>) result.get("copies");
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0)).doesNotContainKey("qrToken");
        assertThat(copies.get(0).get("copyNumber")).isEqualTo("C-001");
        assertThat(copies.get(0).get("status")).isEqualTo("AVAILABLE");
    }

    // F. getSimilarBooks execution
    @Test
    @DisplayName("F. Verify getSimilarBooks excludes the reference book itself")
    void testGetSimilarBooksExecution() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(geminiClient.generateEmbedding(anyString())).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        ScoredPoint pt1 = ScoredPoint.builder().id(1L).score(0.99).build(); // reference book itself
        ScoredPoint pt2 = ScoredPoint.builder().id(2L).score(0.85).build(); // similar book
        when(qdrantVectorService.search(any(), anyInt())).thenReturn(List.of(pt1, pt2));

        Book similarBook = Book.builder()
                .id(2L)
                .title("The Pragmatic Programmer")
                .author("Andy Hunt")
                .availableCopies(1)
                .build();
        when(bookRepository.findAllById(List.of(2L))).thenReturn(List.of(similarBook));

        Map<String, Object> result = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_GET_SIMILAR_BOOKS,
                Map.of("bookId", 1L, "limit", 3)
        );

        assertThat(result.get("referenceBookId")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> similar = (List<Map<String, Object>>) result.get("similarBooks");
        assertThat(similar).hasSize(1);
        assertThat(similar.get(0).get("bookId")).isEqualTo(2L);
        assertThat(similar.get(0).get("title")).isEqualTo("The Pragmatic Programmer");
    }

    // G. getMyBorrowings authentication
    @Test
    @DisplayName("G. Verify getMyBorrowings uses SecurityContext and blocks unauthenticated caller")
    void testGetMyBorrowingsAuthentication() {
        // Unauthenticated call
        Map<String, Object> unauth = toolExecutor.executeTool(SmartLibToolDefinitions.TOOL_GET_MY_BORROWINGS, Map.of());
        assertThat(unauth).containsKey("error");
        assertThat((String) unauth.get("error")).containsIgnoringCase("authentication required");

        // Authenticated call
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        Borrowing b = Borrowing.builder()
                .id(50L)
                .book(testBook)
                .user(testUser)
                .borrowDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(9))
                .status(BorrowStatus.BORROWED)
                .build();
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(b));

        Map<String, Object> authResult = toolExecutor.executeTool(SmartLibToolDefinitions.TOOL_GET_MY_BORROWINGS, Map.of());
        assertThat(authResult).doesNotContainKey("error");
        assertThat(authResult.get("count")).isEqualTo(1);
    }

    // H. getMyOverdueBooks authentication
    @Test
    @DisplayName("H. Verify getMyOverdueBooks calculates overdue days from SecurityContext user")
    void testGetMyOverdueBooks() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        Borrowing overdue = Borrowing.builder()
                .id(51L)
                .book(testBook)
                .user(testUser)
                .borrowDate(LocalDate.now().minusDays(20))
                .dueDate(LocalDate.now().minusDays(6))
                .status(BorrowStatus.OVERDUE)
                .build();
        when(borrowingRepository.findByUserId(100L)).thenReturn(List.of(overdue));

        Map<String, Object> result = toolExecutor.executeTool(SmartLibToolDefinitions.TOOL_GET_MY_OVERDUE_BOOKS, Map.of());
        assertThat(result.get("count")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("overdueBooks");
        assertThat(list.get(0).get("overdueDays")).isEqualTo(6L);
    }

    // I. getMyReservations authentication
    @Test
    @DisplayName("I. Verify getMyReservations resolves user from SecurityContext")
    void testGetMyReservations() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        Reservation res = Reservation.builder()
                .id(70L)
                .book(testBook)
                .user(testUser)
                .status(ReservationStatus.WAITING)
                .reservedAt(LocalDateTime.now().minusDays(2))
                .build();
        when(reservationRepository.findByUserId(100L)).thenReturn(List.of(res));

        Map<String, Object> result = toolExecutor.executeTool(SmartLibToolDefinitions.TOOL_GET_MY_RESERVATIONS, Map.of());
        assertThat(result.get("count")).isEqualTo(1);
    }

    // J. getMyFines authentication
    @Test
    @DisplayName("J. Verify getMyFines sums unpaid balances for authenticated user")
    void testGetMyFines() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        Fine fine = Fine.builder()
                .id(80L)
                .user(testUser)
                .amount(15.50)
                .status(FineStatus.UNPAID)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();
        when(fineRepository.findByUserId(100L)).thenReturn(List.of(fine));

        Map<String, Object> result = toolExecutor.executeTool(SmartLibToolDefinitions.TOOL_GET_MY_FINES, Map.of());
        assertThat(result.get("count")).isEqualTo(1);
        assertThat(result.get("totalUnpaidAmount")).isEqualTo(15.50);
    }

    // K. getPersonalizedRecommendations authentication
    @Test
    @DisplayName("K. Verify getPersonalizedRecommendations uses authenticated user and delegates to service")
    void testGetPersonalizedRecommendations() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")))
        );

        when(personalizedRecommendationService.getRecommendations(testUser, 5)).thenReturn(List.of(
                PersonalizedRecommendation.builder()
                        .bookId(2L)
                        .title("Refactoring")
                        .author("Martin Fowler")
                        .availableCopies(1)
                        .reasonSignals(List.of("Based on your interest in software architecture"))
                        .build()
        ));

        Map<String, Object> result = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_GET_PERSONALIZED_RECOMMENDATIONS,
                Map.of("limit", 5)
        );

        assertThat(result.get("count")).isEqualTo(1);
        verify(personalizedRecommendationService).getRecommendations(testUser, 5);
    }

    // L. Unknown tool rejection
    @Test
    @DisplayName("L. Verify unknown tool names are rejected gracefully with a controlled error")
    void testUnknownToolRejection() {
        Map<String, Object> result = toolExecutor.executeTool("nonExistentTool", Map.of());
        assertThat(result).containsKey("error");
        assertThat((String) result.get("error")).contains("Unknown tool: nonExistentTool");
    }

    // M. Invalid arguments rejection
    @Test
    @DisplayName("M. Verify invalid or missing required tool arguments return controlled error")
    void testInvalidArgumentsRejection() {
        // Missing query
        Map<String, Object> searchRes = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_SEARCH_BOOKS,
                Map.of()
        );
        assertThat(searchRes).containsKey("error");
        assertThat((String) searchRes.get("error")).contains("The 'query' parameter is required");

        // Missing bookId
        Map<String, Object> detailsRes = toolExecutor.executeTool(
                SmartLibToolDefinitions.TOOL_GET_BOOK_DETAILS,
                Map.of()
        );
        assertThat(detailsRes).containsKey("error");
        assertThat((String) detailsRes.get("error")).contains("Valid numeric 'bookId' is required");
    }
}
