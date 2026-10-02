package com.smartlib.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.GroqAiProperties;
import com.smartlib.ai.dto.gemini.FunctionDeclaration;
import com.smartlib.ai.model.AiConversationTurn;
import com.smartlib.ai.model.AiModelRequest;
import com.smartlib.ai.model.AiModelResponse;
import com.smartlib.ai.model.AiProviderCapabilities;
import com.smartlib.ai.model.AiToolCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Component("groqAiModelProvider")
@RequiredArgsConstructor
@Slf4j
public class GroqAiModelProvider implements AiModelProvider {

    public static final String PROVIDER_NAME = "groq";

    private final RestClient groqRestClient;
    private final GroqAiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getModelName() {
        return properties.getModel();
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    @Override
    public AiProviderCapabilities getCapabilities() {
        return AiProviderCapabilities.builder()
                .supportsToolCalling(true)
                .supportsWebGrounding(false)
                .build();
    }

    @Override
    public AiModelResponse generateChat(AiModelRequest request) {
        if (!isAvailable()) {
            log.warn("Groq chat requested but GROQ_API_KEY is not configured or provider is disabled.");
            throw new IllegalStateException("Groq AI service is not configured or disabled. GROQ_API_KEY is missing.");
        }

        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String responseJson = groqRestClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseJson);

        } catch (RestClientResponseException ex) {
            log.error("Groq chat API error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Groq API request failed with status: " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Groq chat communication error: {}", ex.getMessage());
            throw new RuntimeException("Failed to communicate with Groq API: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> buildRequestBody(AiModelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();

        if (request.getSystemInstruction() != null && !request.getSystemInstruction().isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", request.getSystemInstruction()
            ));
        }

        if (request.getTurns() != null) {
            for (AiConversationTurn turn : request.getTurns()) {
                String role = turn.getRole();
                if ("user".equalsIgnoreCase(role)) {
                    messages.add(Map.of(
                            "role", "user",
                            "content", turn.getContent() != null ? turn.getContent() : ""
                    ));
                } else if ("model".equalsIgnoreCase(role) || "assistant".equalsIgnoreCase(role)) {
                    Map<String, Object> assistantMessage = new LinkedHashMap<>();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", turn.getContent() != null ? turn.getContent() : "");

                    if (turn.getToolCalls() != null && !turn.getToolCalls().isEmpty()) {
                        List<Map<String, Object>> toolCalls = new ArrayList<>();
                        for (int i = 0; i < turn.getToolCalls().size(); i++) {
                            AiToolCall call = turn.getToolCalls().get(i);
                            Map<String, Object> callObj = new LinkedHashMap<>();
                            callObj.put("id", call.getId() != null ? call.getId() : ("call_" + (i + 1)));
                            callObj.put("type", "function");

                            Map<String, Object> functionObj = new LinkedHashMap<>();
                            functionObj.put("name", call.getName());
                            try {
                                functionObj.put("arguments", objectMapper.writeValueAsString(
                                        call.getArguments() != null ? call.getArguments() : Collections.emptyMap()
                                ));
                            } catch (Exception e) {
                                functionObj.put("arguments", "{}");
                            }
                            callObj.put("function", functionObj);
                            toolCalls.add(callObj);
                        }
                        assistantMessage.put("tool_calls", toolCalls);
                    }
                    messages.add(assistantMessage);

                } else if ("tool".equalsIgnoreCase(role)) {
                    Map<String, Object> toolMessage = new LinkedHashMap<>();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", turn.getToolCallId() != null ? turn.getToolCallId() : "call_1");
                    if (turn.getToolName() != null) {
                        toolMessage.put("name", turn.getToolName());
                    }
                    try {
                        toolMessage.put("content", objectMapper.writeValueAsString(
                                turn.getToolResult() != null ? turn.getToolResult() : Collections.emptyMap()
                        ));
                    } catch (Exception e) {
                        toolMessage.put("content", "{}");
                    }
                    messages.add(toolMessage);
                }
            }
        }

        body.put("messages", messages);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (FunctionDeclaration declaration : request.getTools()) {
                tools.add(convertToolSchema(declaration));
            }
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        return body;
    }

    public Map<String, Object> convertToolSchema(FunctionDeclaration declaration) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", declaration.getName());
        if (declaration.getDescription() != null) {
            function.put("description", declaration.getDescription());
        }

        if (declaration.getParameters() != null) {
            function.put("parameters", normalizeSchema(declaration.getParameters()));
        } else {
            function.put("parameters", Map.of("type", "object", "properties", Collections.emptyMap()));
        }

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeSchema(Object node) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();

                if ("type".equalsIgnoreCase(key) && val instanceof String typeStr) {
                    normalized.put("type", typeStr.toLowerCase(Locale.ROOT));
                } else {
                    normalized.put(key, normalizeSchema(val));
                }
            }
            return normalized;
        } else if (node instanceof List<?> list) {
            List<Object> normalizedList = new ArrayList<>();
            for (Object item : list) {
                normalizedList.add(normalizeSchema(item));
            }
            return normalizedList;
        }
        return node;
    }

    public AiModelResponse parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return AiModelResponse.builder()
                    .provider(PROVIDER_NAME)
                    .model(getModelName())
                    .text("")
                    .toolCalls(Collections.emptyList())
                    .build();
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return AiModelResponse.builder()
                        .provider(PROVIDER_NAME)
                        .model(getModelName())
                        .text("")
                        .toolCalls(Collections.emptyList())
                        .build();
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            String finishReason = firstChoice.path("finish_reason").asText(null);

            String text = null;
            if (message.has("content") && !message.path("content").isNull()) {
                text = message.path("content").asText();
            }

            List<AiToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = message.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tcNode : toolCallsNode) {
                    String callId = tcNode.path("id").asText(null);
                    JsonNode funcNode = tcNode.path("function");
                    String funcName = funcNode.path("name").asText();
                    String argsRaw = funcNode.path("arguments").asText("{}");

                    Map<String, Object> arguments;
                    try {
                        arguments = objectMapper.readValue(argsRaw, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception ex) {
                        log.warn("Failed to parse tool call arguments as JSON: {}", argsRaw);
                        arguments = Collections.emptyMap();
                    }

                    toolCalls.add(AiToolCall.builder()
                            .id(callId)
                            .name(funcName)
                            .arguments(arguments)
                            .build());
                }
            }

            return AiModelResponse.builder()
                    .provider(PROVIDER_NAME)
                    .model(root.path("model").asText(getModelName()))
                    .text(text)
                    .toolCalls(toolCalls)
                    .finishReason(finishReason)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to parse Groq response JSON: {}", ex.getMessage());
            throw new RuntimeException("Failed to parse Groq response: " + ex.getMessage(), ex);
        }
    }
}
