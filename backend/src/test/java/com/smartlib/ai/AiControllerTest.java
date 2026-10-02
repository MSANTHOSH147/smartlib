package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.dto.AiChatRequest;
import com.smartlib.ai.dto.AiOrchestrationResult;
import com.smartlib.ai.dto.ChatMessageDto;
import com.smartlib.ai.security.AiRateLimiter;
import com.smartlib.ai.service.SmartLibAiOrchestrator;
import com.smartlib.config.SecurityConfig;
import com.smartlib.controller.AiController;
import com.smartlib.entity.User;
import com.smartlib.enums.Role;
import com.smartlib.exception.GlobalExceptionHandler;
import com.smartlib.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private SmartLibAiOrchestrator orchestrator;

    @Mock
    private AiRateLimiter rateLimiter;

    @InjectMocks
    private AiController aiController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private UsernamePasswordAuthenticationToken authToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = User.builder()
                .id(42L)
                .name("Alice Member")
                .email("alice@smartlib.com")
                .role(Role.MEMBER)
                .active(true)
                .build();

        authToken = new UsernamePasswordAuthenticationToken(
                testUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }

    // A. Authenticated request succeeds
    @Test
    @DisplayName("A. Authenticated request succeeds with HTTP 200 and clean response")
    void testAuthenticatedRequestSucceeds() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Do we have Clean Code?")
                .build();

        when(orchestrator.chat(eq("Do we have Clean Code?"), any()))
                .thenReturn(AiOrchestrationResult.success(
                        "Yes, SmartLib currently has 2 copies of Clean Code available.",
                        List.of("searchBooks", "checkBookAvailability")
                ));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.reply", containsString("Clean Code")))
                .andExpect(jsonPath("$.toolsExecuted", hasItems("searchBooks", "checkBookAvailability")));
    }

    // B. Unauthenticated request rejected
    @Test
    @DisplayName("B. Unauthenticated request is rejected with HTTP 401 Unauthorized")
    void testUnauthenticatedRequestRejected() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Do we have Clean Code?")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orchestrator);
    }

    // C. Blank message rejected
    @Test
    @DisplayName("C. Blank message rejected with HTTP 400 Bad Request")
    void testBlankMessageRejected() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("   ")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Message must not be blank")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verifyNoInteractions(orchestrator);
    }

    // D. Message over 1000 characters rejected
    @Test
    @DisplayName("D. Message over 1000 characters rejected with HTTP 400")
    void testOversizedMessageRejected() throws Exception {
        String longText = "x".repeat(1001);
        AiChatRequest request = AiChatRequest.builder()
                .message(longText)
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("1000 characters")));

        verifyNoInteractions(orchestrator);
    }

    // E. Valid history accepted
    @Test
    @DisplayName("E. Valid history is converted and passed to orchestrator")
    void testValidHistoryAccepted() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Do you have that?")
                .history(List.of(
                        ChatMessageDto.builder().role("user").content("Tell me about Clean Architecture").build(),
                        ChatMessageDto.builder().role("model").content("It is a software architecture book.").build()
                ))
                .build();

        when(orchestrator.chat(eq("Do you have that?"), any()))
                .thenReturn(AiOrchestrationResult.success("Yes, we have 1 copy.", List.of("searchBooks")));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(orchestrator).chat(eq("Do you have that?"), argThat(h -> h.size() == 2));
    }

    // F. Oversized/malformed history rejected
    @Test
    @DisplayName("F. Malformed history role rejected with HTTP 400")
    void testMalformedHistoryRejected() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Hello")
                .history(List.of(
                        ChatMessageDto.builder().role("invalid_role").content("Some text").build()
                ))
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid history role")));

        verifyNoInteractions(orchestrator);
    }

    // G. Orchestrator called with validated data
    @Test
    @DisplayName("G. Orchestrator receives cleaned message and valid arguments")
    void testOrchestratorCalledWithValidatedData() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Recommend a science fiction book")
                .build();

        when(orchestrator.chat(eq("Recommend a science fiction book"), any()))
                .thenReturn(AiOrchestrationResult.success("I recommend Dune by Frank Herbert.", List.of()));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(orchestrator).chat("Recommend a science fiction book", List.of());
    }

    // H. Successful orchestration mapped correctly
    @Test
    @DisplayName("H. Successful orchestration mapped correctly into response DTO")
    void testSuccessfulOrchestrationMappedCorrectly() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("What are my fines?")
                .build();

        when(orchestrator.chat(anyString(), any()))
                .thenReturn(AiOrchestrationResult.success("You have no unpaid fines.", List.of("getMyFines")));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("You have no unpaid fines.")))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.toolsExecuted", hasItem("getMyFines")));
    }

    // I. Orchestrator failure handled safely
    @Test
    @DisplayName("I. Orchestrator failure handled safely without breaking API contract")
    void testOrchestratorFailureHandledSafely() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Search for a book")
                .build();

        when(orchestrator.chat(anyString(), any()))
                .thenReturn(AiOrchestrationResult.error(
                        "Gemini API timed out",
                        "I'm having trouble connecting to the library assistant service. Please try again in a moment."
                ));

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.reply", containsString("having trouble connecting")));
    }

    // J & K. Rate limit rejection with HTTP 429
    @Test
    @DisplayName("K. Rate limit exceeded returns HTTP 429 Too Many Requests")
    void testRateLimitRejectionReturns429() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Spamming queries")
                .build();

        doThrow(new RateLimitExceededException("Rate limit exceeded: Maximum 10 AI chat requests per minute."))
                .when(rateLimiter).checkRateLimit("user:42");

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));

        verifyNoInteractions(orchestrator);
    }

    // M. Error responses do not expose stack traces
    @Test
    @DisplayName("M. Error responses do not leak stack traces")
    void testErrorResponsesDoNotExposeStackTraces() throws Exception {
        AiChatRequest request = AiChatRequest.builder().message("").build();

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // N. Error responses do not expose secrets
    @Test
    @DisplayName("N. Error responses do not expose secrets or API keys")
    void testErrorResponsesDoNotExposeSecrets() throws Exception {
        doThrow(new RuntimeException("Secret key: AIzaSyD-sample-fake-key"))
                .when(rateLimiter).checkRateLimit(anyString());

        mockMvc.perform(post("/api/ai/chat")
                        .principal(authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiChatRequest.builder().message("Test").build())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Internal server error")))
                .andExpect(jsonPath("$.message", not(containsString("AIzaSy"))));
    }

    // O. Existing CORS/security configuration remains valid
    @Test
    @DisplayName("O. Existing CORS configuration allows allowed origins and does not use wildcard")
    void testCorsConfigurationRemainsValid() {
        SecurityConfig securityConfig = new SecurityConfig(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/ai/chat");

        CorsConfiguration cors = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains(
                "http://localhost:5173",
                "http://localhost:5174",
                "https://smartlib-frontend-nmml.onrender.com"
        );
        assertThat(cors.getAllowedOrigins()).doesNotContain("*");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
