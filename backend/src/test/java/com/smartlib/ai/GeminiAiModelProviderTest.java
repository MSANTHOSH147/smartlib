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
}
