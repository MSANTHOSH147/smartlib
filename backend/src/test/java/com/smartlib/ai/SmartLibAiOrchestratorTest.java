package com.smartlib.ai;

import com.smartlib.ai.dto.AiOrchestrationResult;
import com.smartlib.ai.dto.gemini.*;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.ai.tools.SmartLibToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartLibAiOrchestratorTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private SmartLibToolExecutor toolExecutor;

    @InjectMocks
    private SmartLibAiOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        lenient().when(geminiClient.isAvailable()).thenReturn(true);
        orchestrator = new SmartLibAiOrchestrator(geminiClient, toolExecutor);
    }

    private GeminiChatResponse createTextResponse(String text) {
        Content content = Content.model(text);
        GeminiChatResponse.Candidate candidate = GeminiChatResponse.Candidate.builder()
                .content(content)
                .build();
        return GeminiChatResponse.builder()
                .candidates(List.of(candidate))
                .build();
    }

    private GeminiChatResponse createFunctionCallResponse(String name, Map<String, Object> args) {
        Part part = Part.fromFunctionCall(name, args);
        Content content = Content.builder()
                .role("model")
                .parts(List.of(part))
                .build();
        GeminiChatResponse.Candidate candidate = GeminiChatResponse.Candidate.builder()
                .content(content)
                .build();
        return GeminiChatResponse.builder()
                .candidates(List.of(candidate))
                .build();
    }

    private GeminiChatResponse createMultiFunctionCallResponse(List<FunctionCall> calls) {
        List<Part> parts = calls.stream()
                .map(c -> Part.fromFunctionCall(c.getName(), c.getArgs()))
                .toList();
        Content content = Content.builder()
                .role("model")
                .parts(parts)
                .build();
        GeminiChatResponse.Candidate candidate = GeminiChatResponse.Candidate.builder()
                .content(content)
                .build();
        return GeminiChatResponse.builder()
                .candidates(List.of(candidate))
                .build();
    }

    // N. Multiple tool calls
    @Test
    @DisplayName("N. Handle multiple tool calls in a single Gemini turn")
    void testMultipleToolCallsInSingleTurn() {
        FunctionCall call1 = FunctionCall.builder().name("searchBooks").args(Map.of("query", "Clean Code")).build();
        FunctionCall call2 = FunctionCall.builder().name("checkBookAvailability").args(Map.of("bookId", 1)).build();

        GeminiChatResponse multiCallResponse = createMultiFunctionCallResponse(List.of(call1, call2));
        GeminiChatResponse finalResponse = createTextResponse("We found Clean Code and have 2 available copies.");

        when(geminiClient.generateChat(any()))
                .thenReturn(multiCallResponse)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Clean Code")))
                .thenReturn(Map.of("results", List.of(Map.of("bookId", 1, "title", "Clean Code"))));
        when(toolExecutor.executeTool("checkBookAvailability", Map.of("bookId", 1)))
                .thenReturn(Map.of("isAvailable", true, "availableCopies", 2));

        AiOrchestrationResult result = orchestrator.chat("Do we have Clean Code and is it available?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).isEqualTo("We found Clean Code and have 2 available copies.");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks", "checkBookAvailability");
        verify(toolExecutor).executeTool("searchBooks", Map.of("query", "Clean Code"));
        verify(toolExecutor).executeTool("checkBookAvailability", Map.of("bookId", 1));
    }

    // O. Tool loop termination
    @Test
    @DisplayName("O. Tool loop terminates cleanly when Gemini produces text")
    void testToolLoopTermination() {
        GeminiChatResponse toolResponse = createFunctionCallResponse("searchBooks", Map.of("query", "Design Patterns"));
        GeminiChatResponse finalResponse = createTextResponse("We have Design Patterns by the Gang of Four.");

        when(geminiClient.generateChat(any()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Design Patterns")))
                .thenReturn(Map.of("results", List.of(Map.of("title", "Design Patterns"))));

        AiOrchestrationResult result = orchestrator.chat("Search for Design Patterns");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("Gang of Four");
        verify(geminiClient, times(2)).generateChat(any());
    }

    // P. Maximum 4 tool loops
    @Test
    @DisplayName("P. Enforce maximum 4 tool loops to avoid infinite recursion")
    void testMaxToolLoopsExceeded() {
        GeminiChatResponse infiniteToolCall = createFunctionCallResponse("searchBooks", Map.of("query", "Loop"));
        when(geminiClient.generateChat(any())).thenReturn(infiniteToolCall);
        when(toolExecutor.executeTool(any(), any())).thenReturn(Map.of("status", "ok"));

        AiOrchestrationResult result = orchestrator.chat("Please keep looping");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("maximum tool call loop limit exceeded");
        assertThat(result.getReply()).contains("unable to complete your request");
        verify(geminiClient, times(4)).generateChat(any());
    }

    // Q. 1000-character input validation
    @Test
    @DisplayName("Q. Reject messages exceeding 1000 characters without calling Gemini")
    void testInputLengthValidation() {
        String longMessage = "a".repeat(1001);
        AiOrchestrationResult result = orchestrator.chat(longMessage);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("1000 characters");
        assertThat(result.getReply()).contains("exceeds the 1000-character limit");
        verifyNoInteractions(geminiClient);
    }

    // R. 8-turn history limit
    @Test
    @DisplayName("R. Sliding window trims history to maximum 8 turns")
    void testHistoryTurnLimit() {
        List<Content> pastTurns = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            pastTurns.add(i % 2 == 1 ? Content.user("Msg " + i) : Content.model("Reply " + i));
        }

        when(geminiClient.generateChat(any())).thenReturn(createTextResponse("Final answer"));

        orchestrator.chat("New message", pastTurns);

        ArgumentCaptor<GeminiChatRequest> captor = ArgumentCaptor.forClass(GeminiChatRequest.class);
        verify(geminiClient).generateChat(captor.capture());

        GeminiChatRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getContents().size()).isLessThanOrEqualTo(8);
    }

    // S. Gemini API failure handling
    @Test
    @DisplayName("S. Catch Gemini API failure gracefully and return controlled error")
    void testGeminiApiFailureHandling() {
        when(geminiClient.generateChat(any())).thenThrow(new RuntimeException("API rate limit exceeded"));

        AiOrchestrationResult result = orchestrator.chat("Who is Isaac Asimov?");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("API rate limit exceeded");
        assertThat(result.getReply()).contains("having trouble connecting");
    }

    // T. Tool execution failure handling
    @Test
    @DisplayName("T. Tool execution failure returns structured error to model and does not crash")
    void testToolExecutionFailureHandling() {
        GeminiChatResponse toolResponse = createFunctionCallResponse("searchBooks", Map.of("query", "Crash"));
        GeminiChatResponse finalResponse = createTextResponse("I'm sorry, I could not query the catalog right now.");

        when(geminiClient.generateChat(any()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Crash")))
                .thenReturn(Map.of("error", "Database timeout"));

        AiOrchestrationResult result = orchestrator.chat("Find Crash book");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("could not query the catalog");
    }

    // U. Empty final response handling
    @Test
    @DisplayName("U. Empty or blank response from Gemini returns controlled error")
    void testEmptyGeminiResponse() {
        when(geminiClient.generateChat(any())).thenReturn(createTextResponse("   "));

        AiOrchestrationResult result = orchestrator.chat("Hello?");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("empty text");
    }

    // V. General question without tools
    @Test
    @DisplayName("V. General knowledge question answered directly without executing tools")
    void testGeneralQuestionWithoutTools() {
        when(geminiClient.generateChat(any())).thenReturn(createTextResponse("George Orwell wrote 1984."));

        AiOrchestrationResult result = orchestrator.chat("Who wrote 1984?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).isEqualTo("George Orwell wrote 1984.");
        assertThat(result.getToolCallsExecuted()).isEmpty();
        verifyNoInteractions(toolExecutor);
    }

    // W. SmartLib question requiring tools
    @Test
    @DisplayName("W. SmartLib catalog query executes tool and returns synthesized result")
    void testSmartLibQuestionRequiringTools() {
        GeminiChatResponse toolCall = createFunctionCallResponse("searchBooks", Map.of("query", "Clean Code"));
        GeminiChatResponse finalResponse = createTextResponse("Yes, we have Clean Code by Robert C. Martin in the library catalog.");

        when(geminiClient.generateChat(any()))
                .thenReturn(toolCall)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Clean Code")))
                .thenReturn(Map.of("results", List.of(Map.of("bookId", 1, "title", "Clean Code", "availableCopies", 2))));

        AiOrchestrationResult result = orchestrator.chat("Do we have Clean Code?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("Robert C. Martin");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks");
    }

    // X. Mixed general + SmartLib question
    @Test
    @DisplayName("X. Mixed general and SmartLib question invokes tool and combines knowledge")
    void testMixedQuestion() {
        GeminiChatResponse toolCall = createFunctionCallResponse("searchBooks", Map.of("query", "Clean Code"));
        GeminiChatResponse finalResponse = createTextResponse("Clean Code is a handbook of agile software craftsmanship. SmartLib currently has 2 copies available on Shelf A1.");

        when(geminiClient.generateChat(any()))
                .thenReturn(toolCall)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Clean Code")))
                .thenReturn(Map.of("results", List.of(Map.of("bookId", 1, "title", "Clean Code", "availableCopies", 2))));

        AiOrchestrationResult result = orchestrator.chat("What is Clean Code about and do we have it?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("software craftsmanship");
        assertThat(result.getReply()).contains("2 copies available");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks");
    }

    // ============================================================
    // Phase 6.5 Orchestrator Regression Tests (21 - 25)
    // ============================================================

    @Test
    @DisplayName("21. Gemini provider still works through router abstraction")
    void testGeminiProviderStillWorks() {
        GeminiChatResponse textResponse = createTextResponse("Gemini provider is active and working.");
        when(geminiClient.generateChat(any())).thenReturn(textResponse);

        AiOrchestrationResult result = orchestrator.chat("What is your current status?");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).isEqualTo("Gemini provider is active and working.");
        assertThat(result.getToolCallsExecuted()).isEmpty();
    }

    @Test
    @DisplayName("22. Fallback provider still supports tools on primary 429 failure")
    void testFallbackProviderStillSupportsTools() {
        com.smartlib.ai.provider.AiModelProvider mockGeminiProvider = mock(com.smartlib.ai.provider.AiModelProvider.class);
        com.smartlib.ai.provider.AiModelProvider mockGroqProvider = mock(com.smartlib.ai.provider.AiModelProvider.class);

        when(mockGeminiProvider.getProviderName()).thenReturn("gemini");
        when(mockGeminiProvider.isAvailable()).thenReturn(true);
        when(mockGeminiProvider.generateChat(any())).thenThrow(new RuntimeException("status: 429 Too Many Requests"));

        when(mockGroqProvider.getProviderName()).thenReturn("groq");
        when(mockGroqProvider.isAvailable()).thenReturn(true);

        // Turn 1: Groq returns tool call
        com.smartlib.ai.model.AiToolCall toolCall = com.smartlib.ai.model.AiToolCall.builder()
                .id("call_groq_1")
                .name("searchBooks")
                .arguments(Map.of("query", "Clean Architecture"))
                .build();
        com.smartlib.ai.model.AiModelResponse toolResponse = com.smartlib.ai.model.AiModelResponse.builder()
                .provider("groq")
                .toolCalls(List.of(toolCall))
                .build();

        // Turn 2: Groq returns final text
        com.smartlib.ai.model.AiModelResponse finalResponse = com.smartlib.ai.model.AiModelResponse.builder()
                .provider("groq")
                .text("Found 3 copies of Clean Architecture in SmartLib.")
                .build();

        when(mockGroqProvider.generateChat(any()))
                .thenReturn(toolResponse)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Clean Architecture")))
                .thenReturn(Map.of("results", List.of(Map.of("title", "Clean Architecture", "availableCopies", 3))));

        com.smartlib.ai.config.AiRoutingProperties routingProps = new com.smartlib.ai.config.AiRoutingProperties();
        routingProps.setPrimaryProvider("gemini");
        routingProps.setFallbackProvider("groq");
        com.smartlib.ai.router.AiModelRouter router = new com.smartlib.ai.router.AiModelRouter(
                List.of(mockGeminiProvider, mockGroqProvider), routingProps
        );

        SmartLibAiOrchestrator fallbackOrchestrator = new SmartLibAiOrchestrator(router, toolExecutor);
        AiOrchestrationResult result = fallbackOrchestrator.chat("Search for Clean Architecture");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).contains("Found 3 copies of Clean Architecture");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks");
        verify(toolExecutor).executeTool("searchBooks", Map.of("query", "Clean Architecture"));
    }

    @Test
    @DisplayName("23. Multi-tool calls still work through router abstraction")
    void testMultiToolCallStillWorks() {
        FunctionCall call1 = FunctionCall.builder().name("searchBooks").args(Map.of("query", "Refactoring")).build();
        FunctionCall call2 = FunctionCall.builder().name("checkBookAvailability").args(Map.of("bookId", 5)).build();

        GeminiChatResponse multiCallResponse = createMultiFunctionCallResponse(List.of(call1, call2));
        GeminiChatResponse finalResponse = createTextResponse("We found Refactoring and it is available.");

        when(geminiClient.generateChat(any()))
                .thenReturn(multiCallResponse)
                .thenReturn(finalResponse);

        when(toolExecutor.executeTool("searchBooks", Map.of("query", "Refactoring")))
                .thenReturn(Map.of("results", List.of(Map.of("bookId", 5, "title", "Refactoring"))));
        when(toolExecutor.executeTool("checkBookAvailability", Map.of("bookId", 5)))
                .thenReturn(Map.of("isAvailable", true, "availableCopies", 1));

        AiOrchestrationResult result = orchestrator.chat("Check Refactoring availability");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReply()).isEqualTo("We found Refactoring and it is available.");
        assertThat(result.getToolCallsExecuted()).containsExactly("searchBooks", "checkBookAvailability");
    }

    @Test
    @DisplayName("24. Four-loop limit still works through router abstraction")
    void testFourLoopLimitStillWorks() {
        GeminiChatResponse infiniteToolCall = createFunctionCallResponse("searchBooks", Map.of("query", "Infinite"));
        when(geminiClient.generateChat(any())).thenReturn(infiniteToolCall);
        when(toolExecutor.executeTool(any(), any())).thenReturn(Map.of("status", "ok"));

        AiOrchestrationResult result = orchestrator.chat("Loop infinitely");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("maximum tool call loop limit exceeded");
    }

    @Test
    @DisplayName("25. Existing security and validation behaviors remain unchanged")
    void testExistingSecurityBehaviorUnchanged() {
        // Empty message check
        AiOrchestrationResult emptyResult = orchestrator.chat("   ");
        assertThat(emptyResult.isSuccess()).isFalse();
        assertThat(emptyResult.getErrorMessage()).containsIgnoringCase("cannot be empty");

        // 1000 character limit check
        String oversized = "x".repeat(1001);
        AiOrchestrationResult lengthResult = orchestrator.chat(oversized);
        assertThat(lengthResult.isSuccess()).isFalse();
        assertThat(lengthResult.getErrorMessage()).contains("1000 characters");

        verifyNoInteractions(geminiClient);
    }
}
