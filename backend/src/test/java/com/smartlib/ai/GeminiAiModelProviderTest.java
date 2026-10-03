package com.smartlib.ai;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.gemini.*;
import com.smartlib.ai.model.AiConversationTurn;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiToolCall;
import com.smartlib.ai.provider.GeminiAiModelProvider;
import com.smartlib.ai.service.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiAiModelProviderTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GeminiAiProperties properties;

    private GeminiAiModelProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getModel()).thenReturn("gemini-3.8-flash");
        lenient().when(geminiClient.isAvailable()).thenReturn(true);
        provider = new GeminiAiModelProvider(geminiClient, properties);
    }

    @Test
    @DisplayName("13. GeminiAiModelProvider delegates chat generation to GeminiClient")
    void testDelegatesToGeminiClient() {
        AiModelRequest request = AiModelRequest.builder()
                .systemInstruction("You are SmartLib AI.")
                .turns(List.of(AiConversationTurn.userTurn("Hello library")))
                .build();

        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.model("Welcome to SmartLib!"))
                                .finishReason("STOP")
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any())).thenReturn(mockResponse);

        AiModelResponse response = provider.generateChat(request);

        assertThat(response).isNotNull();
        assertThat(response.getProvider()).isEqualTo("gemini");
        assertThat(response.getModel()).isEqualTo("gemini-3.8-flash");
        assertThat(response.getText()).isEqualTo("Welcome to SmartLib!");

        ArgumentCaptor<GeminiChatRequest> captor = ArgumentCaptor.forClass(GeminiChatRequest.class);
        verify(geminiClient, times(1)).generateChat(captor.capture());
        GeminiChatRequest sent = captor.getValue();
        assertThat(sent.getSystemInstruction().getParts().get(0).getText()).isEqualTo("You are SmartLib AI.");
        assertThat(sent.getContents().get(0).getParts().get(0).getText()).isEqualTo("Hello library");
    }

    @Test
    @DisplayName("14. Function calls in Gemini response are normalized to AiToolCall objects")
    void testToolCallsNormalized() {
        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(AiConversationTurn.userTurn("Check book availability")))
                .build();

        Part functionPart = Part.fromFunctionCall("checkBookAvailability", Map.of("bookId", 42));
        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.builder().role("model").parts(List.of(functionPart)).build())
                                .finishReason("STOP")
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any())).thenReturn(mockResponse);

        AiModelResponse response = provider.generateChat(request);

        assertThat(response.hasToolCalls()).isTrue();
        assertThat(response.getToolCalls()).hasSize(1);

        AiToolCall toolCall = response.getToolCalls().get(0);
        assertThat(toolCall.getName()).isEqualTo("checkBookAvailability");
        assertThat(toolCall.getArguments()).containsEntry("bookId", 42);
    }

    @Test
    @DisplayName("15. Text response from Gemini is normalized into AiModelResponse")
    void testTextResponseNormalized() {
        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(AiConversationTurn.userTurn("Who wrote Clean Code?")))
                .build();

        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.model("Robert C. Martin wrote Clean Code."))
                                .finishReason("STOP")
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any())).thenReturn(mockResponse);

        AiModelResponse response = provider.generateChat(request);

        assertThat(response.hasText()).isTrue();
        assertThat(response.getText()).isEqualTo("Robert C. Martin wrote Clean Code.");
        assertThat(response.getFinishReason()).isEqualTo("STOP");
        assertThat(response.hasToolCalls()).isFalse();
    }

    @Test
    @DisplayName("16. Tool execution turns map to Gemini Content with role 'user' and functionResponse part")
    void testToolExecutionTurnMapsToUserRoleWithFunctionResponse() {
        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(
                        AiConversationTurn.userTurn("What books are available?"),
                        AiConversationTurn.builder()
                                .role("model")
                                .toolCalls(List.of(AiToolCall.builder().name("searchBooks").arguments(Map.of("query", "available")).build()))
                                .build(),
                        AiConversationTurn.toolTurn("call_1", "searchBooks", Map.of("results", List.of("Clean Code")))
                ))
                .build();

        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.model("Clean Code is available."))
                                .finishReason("STOP")
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any())).thenReturn(mockResponse);

        provider.generateChat(request);

        ArgumentCaptor<GeminiChatRequest> captor = ArgumentCaptor.forClass(GeminiChatRequest.class);
        verify(geminiClient).generateChat(captor.capture());

        GeminiChatRequest sentRequest = captor.getValue();
        List<Content> contents = sentRequest.getContents();
        assertThat(contents).hasSize(3);

        // Turn 1: user
        assertThat(contents.get(0).getRole()).isEqualTo("user");
        assertThat(contents.get(0).getParts().get(0).getText()).isEqualTo("What books are available?");

        // Turn 2: model
        assertThat(contents.get(1).getRole()).isEqualTo("model");
        assertThat(contents.get(1).getParts().get(0).getFunctionCall().getName()).isEqualTo("searchBooks");

        // Turn 3: tool result MUST have role 'user' in Gemini
        assertThat(contents.get(2).getRole()).isEqualTo("user");
        assertThat(contents.get(2).getParts().get(0).getFunctionResponse()).isNotNull();
        assertThat(contents.get(2).getParts().get(0).getFunctionResponse().getName()).isEqualTo("searchBooks");
        assertThat(contents.get(2).getParts().get(0).getFunctionResponse().getResponse()).containsEntry("results", List.of("Clean Code"));
    }

    @Test
    @DisplayName("17. Assistant role turn is mapped explicitly to 'model' for Gemini")
    void testAssistantRoleTurnMappedToModel() {
        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(
                        AiConversationTurn.userTurn("Hi"),
                        AiConversationTurn.builder().role("assistant").content("Hello! How can I help?").build()
                ))
                .build();

        GeminiChatResponse mockResponse = GeminiChatResponse.builder()
                .candidates(List.of(
                        GeminiChatResponse.Candidate.builder()
                                .content(Content.model("Ask me anything!"))
                                .build()
                ))
                .build();

        when(geminiClient.generateChat(any())).thenReturn(mockResponse);

        provider.generateChat(request);

        ArgumentCaptor<GeminiChatRequest> captor = ArgumentCaptor.forClass(GeminiChatRequest.class);
        verify(geminiClient).generateChat(captor.capture());

        GeminiChatRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getContents().get(1).getRole()).isEqualTo("model");
    }

    @Test
    @DisplayName("18. Serialized Gemini request JSON NEVER contains role 'function'")
    void testSerializedGeminiPayloadNeverContainsRoleFunction() throws Exception {
        AiModelRequest request = AiModelRequest.builder()
                .systemInstruction("You are SmartLib AI.")
                .turns(List.of(
                        AiConversationTurn.userTurn("Find Clean Code"),
                        AiConversationTurn.builder()
                                .role("model")
                                .toolCalls(List.of(AiToolCall.builder().name("searchBooks").arguments(Map.of("query", "Clean Code")).build()))
                                .build(),
                        AiConversationTurn.toolTurn("call_1", "searchBooks", Map.of("found", true))
                ))
                .build();

        GeminiChatRequest geminiRequest = provider.toGeminiChatRequest(request);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = mapper.writeValueAsString(geminiRequest);

        assertThat(json).doesNotContain("\"role\":\"function\"");
        assertThat(json).doesNotContain("\"role\":\"assistant\"");
        assertThat(json).contains("\"role\":\"user\"");
        assertThat(json).contains("\"role\":\"model\"");
        assertThat(json).contains("\"functionResponse\"");
    }

    @Test
    @DisplayName("19. Web grounding tool is added when requested")
    void testWebGroundingStillWorksIfApplicable() {
        AiModelRequest request = AiModelRequest.builder()
                .turns(List.of(AiConversationTurn.userTurn("What is the latest library release?")))
                .useWebGrounding(true)
                .build();

        GeminiChatRequest geminiRequest = provider.toGeminiChatRequest(request);
        assertThat(geminiRequest.getTools()).isNotNull();
        assertThat(geminiRequest.getTools().stream().anyMatch(t -> t.getGoogleSearch() != null)).isTrue();
    }

    @Test
    @DisplayName("20. Memory injection in system instructions is preserved")
    void testMemoryInjectionStillWorks() {
        String systemWithMemory = "You are SmartLib AI.\n\n<USER_MEMORY>\n- Prefers Java and Spring Boot\n</USER_MEMORY>";
        AiModelRequest request = AiModelRequest.builder()
                .systemInstruction(systemWithMemory)
                .turns(List.of(AiConversationTurn.userTurn("Recommend a book")))
                .build();

        GeminiChatRequest geminiRequest = provider.toGeminiChatRequest(request);
        assertThat(geminiRequest.getSystemInstruction()).isNotNull();
        assertThat(geminiRequest.getSystemInstruction().getParts().get(0).getText()).contains("<USER_MEMORY>");
        assertThat(geminiRequest.getSystemInstruction().getParts().get(0).getText()).contains("Prefers Java and Spring Boot");
    }
}
