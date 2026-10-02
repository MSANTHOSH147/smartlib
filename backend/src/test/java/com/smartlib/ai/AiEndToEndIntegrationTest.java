package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.AiRoutingProperties;
import com.smartlib.ai.dto.AiChatRequest;
import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiToolCall;
import com.smartlib.ai.provider.AiModelProvider;
import com.smartlib.ai.router.AiModelRouter;
import com.smartlib.ai.security.AiRateLimiter;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.HybridBookSearchService;
import com.smartlib.ai.service.PersonalizedRecommendationService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.ai.tools.SmartLibToolDefinitions;
import com.smartlib.ai.tools.SmartLibToolExecutor;
import com.smartlib.controller.AiController;
import com.smartlib.entity.Book;
import com.smartlib.entity.Fine;
import com.smartlib.entity.User;
import com.smartlib.enums.FineStatus;
import com.smartlib.enums.Role;
import com.smartlib.exception.GlobalExceptionHandler;
import com.smartlib.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiEndToEndIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AiModelProvider geminiProvider;

    @Mock
    private AiModelProvider groqProvider;

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

    private AiRateLimiter rateLimiter;
    private SmartLibToolExecutor toolExecutor;
    private AiModelRouter modelRouter;
    private SmartLibAiOrchestrator orchestrator;
    private AiController aiController;

    private User authMember;
    private UsernamePasswordAuthenticationToken authToken;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        authMember = User.builder()
                .id(42L)
                .name("Jane Austen")
                .email("jane@smartlib.test")
                .role(Role.MEMBER)
                .build();

        authToken = new UsernamePasswordAuthenticationToken(
                authMember, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        sampleBook = Book.builder()
                .id(1L)
                .title("Pride and Prejudice")
                .author("Jane Austen")
                .availableCopies(2)
                .totalCopies(3)
                .build();

        rateLimiter = new AiRateLimiter();

        toolExecutor = new SmartLibToolExecutor(
                hybridBookSearchService,
                personalizedRecommendationService,
                bookRepository,
                bookCopyRepository,
                borrowingRepository,
                reservationRepository,
                fineRepository,
                userRepository,
                geminiClient,
                qdrantVectorService
        );

        lenient().when(geminiProvider.getProviderName()).thenReturn("gemini");
        lenient().when(geminiProvider.isAvailable()).thenReturn(true);
        lenient().when(geminiProvider.getModelName()).thenReturn("gemini-3.8-flash");

        lenient().when(groqProvider.getProviderName()).thenReturn("groq");
        lenient().when(groqProvider.isAvailable()).thenReturn(true);
        lenient().when(groqProvider.getModelName()).thenReturn("llama-3.3-70b-versatile");

        AiRoutingProperties routingProperties = new AiRoutingProperties();
        routingProperties.setPrimaryProvider("gemini");
        routingProperties.setFallbackProvider("groq");
        routingProperties.setCooldownSeconds(60);

        modelRouter = new AiModelRouter(List.of(geminiProvider, groqProvider), routingProperties);

        orchestrator = new SmartLibAiOrchestrator(modelRouter, toolExecutor);

        aiController = new AiController(orchestrator, rateLimiter);

        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        rateLimiter.reset();
    }

    // ============================================================
    // TEST 1: Authenticated member -> POST /api/ai/chat -> tool -> MySQL -> response
    // ============================================================
    @Test
    @DisplayName("TEST 1. Authenticated member executes chat with tool call to MySQL data")
    void testAuthenticatedMemberChatWithMySqlToolCall() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Do we have Pride and Prejudice in the catalog?")
                .build();

        // Turn 1: Gemini requests searchBooks tool
        AiToolCall searchCall = AiToolCall.builder()
                .id("call_1")
                .name(SmartLibToolDefinitions.TOOL_SEARCH_BOOKS)
                .arguments(Map.of("query", "Pride and Prejudice"))
                .build();

        AiModelResponse turn1Response = AiModelResponse.builder()
                .provider("gemini")
                .model("gemini-3.8-flash")
                .toolCalls(List.of(searchCall))
                .build();

        // Turn 2: Gemini returns final synthesized text
        AiModelResponse turn2Response = AiModelResponse.builder()
                .provider("gemini")
                .model("gemini-3.8-flash")
                .text("Yes! We have Pride and Prejudice by Jane Austen in our catalog with 2 copies available.")
                .build();

        when(geminiProvider.generateChat(any(AiModelRequest.class)))
                .thenReturn(turn1Response)
                .thenReturn(turn2Response);

        when(hybridBookSearchService.search("Pride and Prejudice", 10)).thenReturn(List.of(
                BookSearchResult.builder()
                        .bookId(1L)
                        .title("Pride and Prejudice")
                        .author("Jane Austen")
                        .availableCopies(2)
                        .totalCopies(3)
                        .build()
        ));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reply", containsString("Pride and Prejudice")))
                .andExpect(jsonPath("$.reply", containsString("2 copies available")))
                .andExpect(jsonPath("$.toolsExecuted", hasItem("searchBooks")));

        verify(hybridBookSearchService).search("Pride and Prejudice", 10);
    }

    // ============================================================
    // TEST 2: Semantic search -> Gemini embedding -> Qdrant -> MySQL hydration -> final response
    // ============================================================
    @Test
    @DisplayName("TEST 2. Semantic search flow: embedding -> Qdrant -> MySQL hydration -> response")
    void testSemanticSearchQdrantMySqlFlow() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Find 19th-century classic literature")
                .build();

        AiToolCall semanticCall = AiToolCall.builder()
                .id("call_sem_1")
                .name(SmartLibToolDefinitions.TOOL_SEMANTIC_SEARCH_BOOKS)
                .arguments(Map.of("query", "19th century classic literature"))
                .build();

        when(geminiProvider.generateChat(any(AiModelRequest.class)))
                .thenReturn(AiModelResponse.builder().provider("gemini").toolCalls(List.of(semanticCall)).build())
                .thenReturn(AiModelResponse.builder().provider("gemini").text("I found Pride and Prejudice by Jane Austen.").build());

        when(geminiClient.generateEmbedding("19th century classic literature"))
                .thenReturn(List.of(0.12f, 0.34f, 0.56f));

        ScoredPoint qdrantPoint = ScoredPoint.builder().id(1L).score(0.92).build();
        when(qdrantVectorService.search(any(), eq(10))).thenReturn(List.of(qdrantPoint));

        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(sampleBook));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reply", containsString("Pride and Prejudice")))
                .andExpect(jsonPath("$.toolsExecuted", hasItem("semanticSearchBooks")));

        verify(geminiClient).generateEmbedding("19th century classic literature");
        verify(qdrantVectorService).search(any(), eq(10));
        verify(bookRepository).findAllById(List.of(1L));
    }

    // ============================================================
    // TEST 3: Personalized recommendation -> borrowing history -> availability -> final response
    // ============================================================
    @Test
    @DisplayName("TEST 3. Personalized recommendation flow uses authenticated user borrowing history")
    void testPersonalizedRecommendationFlow() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Recommend books based on my reading history")
                .build();

        AiToolCall recCall = AiToolCall.builder()
                .id("call_rec_1")
                .name(SmartLibToolDefinitions.TOOL_GET_PERSONALIZED_RECOMMENDATIONS)
                .arguments(Map.of("limit", 5))
                .build();

        when(geminiProvider.generateChat(any(AiModelRequest.class)))
                .thenReturn(AiModelResponse.builder().provider("gemini").toolCalls(List.of(recCall)).build())
                .thenReturn(AiModelResponse.builder().provider("gemini").text("Based on your history, I recommend Sense and Sensibility.").build());

        when(personalizedRecommendationService.getRecommendations(authMember, 5)).thenReturn(List.of(
                PersonalizedRecommendation.builder()
                        .bookId(2L)
                        .title("Sense and Sensibility")
                        .author("Jane Austen")
                        .availableCopies(1)
                        .reasonSignals(List.of("Based on your past borrowings"))
                        .build()
        ));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reply", containsString("Sense and Sensibility")))
                .andExpect(jsonPath("$.toolsExecuted", hasItem("getPersonalizedRecommendations")));

        verify(personalizedRecommendationService).getRecommendations(authMember, 5);
    }

    // ============================================================
    // TEST 4: Gemini provider failure -> router -> Groq fallback -> final response
    // ============================================================
    @Test
    @DisplayName("TEST 4. Gemini 429 rate limit triggers Groq fallback returning seamless final response")
    void testGeminiFailureRoutesToGroqFallback() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Explain romantic literature")
                .build();

        // Gemini fails with provider rate limit (429)
        when(geminiProvider.generateChat(any(AiModelRequest.class)))
                .thenThrow(new RuntimeException("Gemini API request failed with status: 429"));

        // Groq successfully generates response
        when(groqProvider.generateChat(any(AiModelRequest.class))).thenReturn(
                AiModelResponse.builder()
                        .provider("groq")
                        .model("llama-3.3-70b-versatile")
                        .text("Romantic literature emphasizes emotion, individualism, and the glorification of nature.")
                        .build()
        );

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reply", containsString("Romantic literature emphasizes emotion")))
                .andExpect(jsonPath("$.reply", not(containsString("Gemini failed"))));

        verify(geminiProvider).generateChat(any());
        verify(groqProvider).generateChat(any());
    }

    // ============================================================
    // TEST 5: Unauthorized request -> rejected before AI execution
    // ============================================================
    @Test
    @DisplayName("TEST 5. Unauthenticated request is rejected with HTTP 401 before AI execution")
    void testUnauthorizedRequestRejectedBeforeAiExecution() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Hello library")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(geminiProvider, never()).generateChat(any());
        verify(groqProvider, never()).generateChat(any());
    }

    // ============================================================
    // TEST 6: Rate limit exceeded -> rejected before expensive provider execution
    // ============================================================
    @Test
    @DisplayName("TEST 6. Exceeded rate limit rejects request with 429 before provider execution")
    void testRateLimitExceededRejectionBeforeExecution() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Spam query")
                .build();

        String userKey = "user:42";
        // Exhaust the 10 requests allowed per minute
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(userKey);
        }

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));

        verify(geminiProvider, never()).generateChat(any());
        verify(groqProvider, never()).generateChat(any());
    }

    // ============================================================
    // TEST 7: Cross-user personal data attempt -> blocked
    // ============================================================
    @Test
    @DisplayName("TEST 7. Cross-user personal data query is blocked and isolated to authenticated user")
    void testCrossUserPersonalDataAttemptBlocked() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Show me member 999's unpaid fines")
                .build();

        // Model attempts to call getMyFines with spoofed userId 999
        AiToolCall finesCall = AiToolCall.builder()
                .id("call_fines_1")
                .name(SmartLibToolDefinitions.TOOL_GET_MY_FINES)
                .arguments(Map.of("userId", 999L, "targetMemberId", 999L))
                .build();

        when(geminiProvider.generateChat(any(AiModelRequest.class)))
                .thenReturn(AiModelResponse.builder().provider("gemini").toolCalls(List.of(finesCall)).build())
                .thenReturn(AiModelResponse.builder().provider("gemini").text("You currently have 0.00 in unpaid fines.").build());

        // Authenticated user 42 has no fines
        when(fineRepository.findByUserId(42L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.toolsExecuted", hasItem("getMyFines")));

        // Verified that database was queried STRICTLY for authenticated user ID 42
        verify(fineRepository).findByUserId(42L);
        verify(fineRepository, never()).findByUserId(999L);
    }
}
