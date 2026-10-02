package com.smartlib.ai;

import com.smartlib.ai.config.AiRoutingProperties;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.provider.AiModelProvider;
import com.smartlib.ai.router.AiModelRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModelRouterTest {

    @Mock
    private AiModelProvider geminiProvider;

    @Mock
    private AiModelProvider groqProvider;

    private AiRoutingProperties routingProperties;
    private AiModelRouter router;

    @BeforeEach
    void setUp() {
        lenient().when(geminiProvider.getProviderName()).thenReturn("gemini");
        lenient().when(geminiProvider.isAvailable()).thenReturn(true);
        lenient().when(geminiProvider.getModelName()).thenReturn("gemini-3.8-flash");
        lenient().when(geminiProvider.getCapabilities()).thenReturn(
                com.smartlib.ai.model.AiProviderCapabilities.builder().supportsToolCalling(true).build()
        );

        lenient().when(groqProvider.getProviderName()).thenReturn("groq");
        lenient().when(groqProvider.isAvailable()).thenReturn(true);
        lenient().when(groqProvider.getModelName()).thenReturn("llama-3.3-70b-versatile");
        lenient().when(groqProvider.getCapabilities()).thenReturn(
                com.smartlib.ai.model.AiProviderCapabilities.builder().supportsToolCalling(true).build()
        );

        routingProperties = new AiRoutingProperties();
        routingProperties.setPrimaryProvider("gemini");
        routingProperties.setFallbackProvider("groq");
        routingProperties.setCooldownSeconds(60);

        router = new AiModelRouter(List.of(geminiProvider, groqProvider), routingProperties);
    }

    @Test
    @DisplayName("1. Primary provider (Gemini) is selected by default")
    void testPrimaryProviderSelected() {
        AiModelRequest request = AiModelRequest.builder().build();
        AiModelProvider selected = router.selectProvider(request);

        assertThat(selected).isSameAs(geminiProvider);
        assertThat(selected.getProviderName()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("2. Successful Gemini response does not invoke fallback provider")
    void testGeminiSuccessDoesNotUseFallback() {
        AiModelRequest request = AiModelRequest.builder().build();
        AiModelResponse geminiResponse = AiModelResponse.builder()
                .provider("gemini")
                .model("gemini-3.8-flash")
                .text("Hello from Gemini")
                .build();

        when(geminiProvider.generateChat(any())).thenReturn(geminiResponse);

        AiModelResponse response = router.execute(request);

        assertThat(response).isSameAs(geminiResponse);
        verify(geminiProvider, times(1)).generateChat(request);
        verify(groqProvider, never()).generateChat(any());
    }

    @Test
    @DisplayName("3. HTTP 429 on primary triggers fallback to Groq")
    void test429TriggersFallback() {
        AiModelRequest request = AiModelRequest.builder().build();
        HttpClientErrorException rateLimitEx = HttpClientErrorException.create(
                HttpStatusCode.valueOf(429),
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiProvider.generateChat(any())).thenThrow(rateLimitEx);

        AiModelResponse groqResponse = AiModelResponse.builder()
                .provider("groq")
                .model("llama-3.3-70b-versatile")
                .text("Fallback response from Groq")
                .build();

        when(groqProvider.generateChat(any())).thenReturn(groqResponse);

        AiModelResponse response = router.execute(request);

        assertThat(response.getProvider()).isEqualTo("groq");
        assertThat(response.getText()).isEqualTo("Fallback response from Groq");
        verify(geminiProvider, times(1)).generateChat(request);
        verify(groqProvider, times(1)).generateChat(request);
    }

    @Test
    @DisplayName("4. Timeout on primary triggers fallback to Groq")
    void testTimeoutTriggersFallback() {
        AiModelRequest request = AiModelRequest.builder().build();
        ResourceAccessException timeoutEx = new ResourceAccessException(
                "Read timed out",
                new SocketTimeoutException("Read timed out")
        );

        when(geminiProvider.generateChat(any())).thenThrow(timeoutEx);

        AiModelResponse groqResponse = AiModelResponse.builder()
                .provider("groq")
                .model("llama-3.3-70b-versatile")
                .text("Response after timeout")
                .build();

        when(groqProvider.generateChat(any())).thenReturn(groqResponse);

        AiModelResponse response = router.execute(request);

        assertThat(response.getProvider()).isEqualTo("groq");
        assertThat(response.getText()).isEqualTo("Response after timeout");
        verify(groqProvider, times(1)).generateChat(request);
    }

    @Test
    @DisplayName("5. HTTP 5xx server error on primary triggers fallback to Groq")
    void test5xxTriggersFallback() {
        AiModelRequest request = AiModelRequest.builder().build();
        HttpServerErrorException serverError = HttpServerErrorException.create(
                HttpStatusCode.valueOf(503),
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(geminiProvider.generateChat(any())).thenThrow(serverError);

        AiModelResponse groqResponse = AiModelResponse.builder()
                .provider("groq")
                .text("Groq handled 503 fallback")
                .build();

        when(groqProvider.generateChat(any())).thenReturn(groqResponse);

        AiModelResponse response = router.execute(request);

        assertThat(response.getText()).isEqualTo("Groq handled 503 fallback");
        verify(groqProvider, times(1)).generateChat(request);
    }

    @Test
    @DisplayName("6. Fallback provider returns normalized response on primary failure")
    void testFallbackProviderSuccess() {
        AiModelRequest request = AiModelRequest.builder().build();
        when(geminiProvider.generateChat(any())).thenThrow(new RuntimeException("Gemini API request failed with status: 500"));

        AiModelResponse groqResponse = AiModelResponse.builder()
                .provider("groq")
                .model("llama-3.3-70b-versatile")
                .text("Normalized Groq reply")
                .build();

        when(groqProvider.generateChat(any())).thenReturn(groqResponse);

        AiModelResponse response = router.execute(request);

        assertThat(response.getProvider()).isEqualTo("groq");
        assertThat(response.getModel()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(response.getText()).isEqualTo("Normalized Groq reply");
    }

    @Test
    @DisplayName("7. When both primary and fallback fail, controlled error is thrown")
    void testBothProvidersFail() {
        AiModelRequest request = AiModelRequest.builder().build();
        when(geminiProvider.generateChat(any())).thenThrow(new RuntimeException("Gemini API request failed with status: 500"));
        when(groqProvider.generateChat(any())).thenThrow(new RuntimeException("Groq API request failed with status: 500"));

        assertThatThrownBy(() -> router.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Both primary and fallback AI providers failed");

        verify(geminiProvider, times(1)).generateChat(request);
        verify(groqProvider, times(1)).generateChat(request);
    }

    @Test
    @DisplayName("8. Disabled primary provider routes directly to fallback")
    void testDisabledPrimaryUsesFallback() {
        when(geminiProvider.isAvailable()).thenReturn(false);

        AiModelRequest request = AiModelRequest.builder().build();
        AiModelProvider selected = router.selectProvider(request);

        assertThat(selected).isSameAs(groqProvider);
        assertThat(selected.getProviderName()).isEqualTo("groq");
    }

    @Test
    @DisplayName("9. Disabled fallback does not execute when primary fails")
    void testDisabledFallbackDoesNotExecute() {
        when(geminiProvider.generateChat(any())).thenThrow(new RuntimeException("Gemini API request failed with status: 429"));
        when(groqProvider.isAvailable()).thenReturn(false);

        AiModelRequest request = AiModelRequest.builder().build();

        assertThatThrownBy(() -> router.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("status: 429");

        verify(geminiProvider, times(1)).generateChat(request);
        verify(groqProvider, never()).generateChat(any());
    }

    @Test
    @DisplayName("10. Application validation error does not trigger fallback")
    void testNoFallbackForApplicationValidationError() {
        AiModelRequest request = AiModelRequest.builder().build();
        when(geminiProvider.generateChat(any())).thenThrow(new IllegalArgumentException("Invalid query parameter"));

        assertThatThrownBy(() -> router.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid query parameter");

        verify(geminiProvider, times(1)).generateChat(request);
        verify(groqProvider, never()).generateChat(any());
    }

    @Test
    @DisplayName("11. Provider failure activates cooldown in health tracker")
    void testProviderHealthCooldown() {
        AiModelRequest request = AiModelRequest.builder().build();
        when(geminiProvider.generateChat(any())).thenThrow(new RuntimeException("status: 503"));
        when(groqProvider.generateChat(any())).thenReturn(AiModelResponse.builder().provider("groq").text("OK").build());

        // First call fails on primary, triggering cooldown
        router.execute(request);
        assertThat(router.isCoolingDown("gemini")).isTrue();

        // Subsequent selectProvider skips primary due to active cooldown and picks Groq
        AiModelProvider nextProvider = router.selectProvider(request);
        assertThat(nextProvider).isSameAs(groqProvider);

        // Reset cooldown restores Gemini
        router.resetHealth("gemini");
        assertThat(router.isCoolingDown("gemini")).isFalse();
        assertThat(router.selectProvider(request)).isSameAs(geminiProvider);
    }

    @Test
    @DisplayName("12. Provider metadata and capabilities are correctly retrieved")
    void testProviderMetadata() {
        AiModelProvider gemini = router.getProvider("gemini");
        AiModelProvider groq = router.getProvider("groq");

        assertThat(gemini).isNotNull();
        assertThat(gemini.getProviderName()).isEqualTo("gemini");
        assertThat(gemini.getModelName()).isEqualTo("gemini-3.8-flash");
        assertThat(gemini.getCapabilities().isSupportsToolCalling()).isTrue();

        assertThat(groq).isNotNull();
        assertThat(groq.getProviderName()).isEqualTo("groq");
        assertThat(groq.getModelName()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(groq.getCapabilities().isSupportsToolCalling()).isTrue();
    }
}
