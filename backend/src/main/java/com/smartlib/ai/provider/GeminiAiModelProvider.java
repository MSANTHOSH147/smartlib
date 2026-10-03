package com.smartlib.ai.provider;

import com.smartlib.ai.config.AiWebGroundingProperties;
import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.dto.gemini.*;
import com.smartlib.ai.model.*;
import com.smartlib.ai.service.AiWebGroundingService;
import com.smartlib.ai.service.GeminiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component("geminiAiModelProvider")
@Slf4j
public class GeminiAiModelProvider implements AiModelProvider {

    public static final String PROVIDER_NAME = "gemini";

    private final GeminiClient geminiClient;
    private final GeminiAiProperties properties;
    private final AiWebGroundingService webGroundingService;

    @Autowired
    public GeminiAiModelProvider(GeminiClient geminiClient,
                                 GeminiAiProperties properties,
                                 AiWebGroundingService webGroundingService) {
        this.geminiClient = geminiClient;
        this.properties = properties;
        this.webGroundingService = webGroundingService;
    }

    /**
     * Backward-compatible constructor for testing and standalone usage.
     */
    public GeminiAiModelProvider(GeminiClient geminiClient, GeminiAiProperties properties) {
        this(geminiClient, properties, new AiWebGroundingService(new AiWebGroundingProperties()));
    }

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
        return geminiClient.isAvailable();
    }

    @Override
    public AiProviderCapabilities getCapabilities() {
        return AiProviderCapabilities.builder()
                .supportsToolCalling(true)
                .supportsWebGrounding(true)
                .build();
    }

    @Override
    public AiModelResponse generateChat(AiModelRequest request) {
        GeminiChatRequest geminiRequest = toGeminiChatRequest(request);
        GeminiChatResponse response = geminiClient.generateChat(geminiRequest);

        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            return AiModelResponse.builder()
                    .provider(PROVIDER_NAME)
                    .model(getModelName())
                    .text("")
                    .toolCalls(Collections.emptyList())
                    .sources(Collections.emptyList())
                    .build();
        }

        String finishReason = response.getCandidates().get(0).getFinishReason();
        String text = response.extractText();

        List<AiToolCall> toolCalls = Collections.emptyList();
        if (response.hasFunctionCalls()) {
            toolCalls = response.extractFunctionCalls().stream()
                    .map(fc -> AiToolCall.builder()
                            .name(fc.getName())
                            .arguments(fc.getArgs() != null ? fc.getArgs() : Collections.emptyMap())
                            .thoughtSignature(fc.getThoughtSignature())
                            .build())
                    .toList();
        }

        List<AiSource> sources = Collections.emptyList();
        if (webGroundingService != null) {
            sources = webGroundingService.extractSources(response);
        }

        return AiModelResponse.builder()
                .provider(PROVIDER_NAME)
                .model(getModelName())
                .text(text)
                .toolCalls(toolCalls)
                .finishReason(finishReason)
                .sources(sources)
                .build();
    }

    public GeminiChatRequest toGeminiChatRequest(AiModelRequest request) {
        Content systemInstruction = null;
        if (request.getSystemInstruction() != null && !request.getSystemInstruction().isBlank()) {
            systemInstruction = Content.system(request.getSystemInstruction());
        }

        List<Content> contents = new ArrayList<>();
        List<Part> pendingToolResponses = new ArrayList<>();

        if (request.getTurns() != null) {
            for (AiConversationTurn turn : request.getTurns()) {
                String turnRole = turn.getRole() != null ? turn.getRole().trim().toLowerCase() : "";
                if ("tool".equals(turnRole) || "function".equals(turnRole)) {
                    pendingToolResponses.add(Part.fromFunctionResponse(turn.getToolName(), turn.getToolResult()));
                } else {
                    if (!pendingToolResponses.isEmpty()) {
                        contents.add(Content.functionResponses(new ArrayList<>(pendingToolResponses)));
                        pendingToolResponses.clear();
                    }

                    if ("user".equals(turnRole)) {
                        contents.add(Content.user(turn.getContent() != null ? turn.getContent() : ""));
                    } else if ("model".equals(turnRole) || "assistant".equals(turnRole)) {
                        if (turn.getToolCalls() != null && !turn.getToolCalls().isEmpty()) {
                            List<Part> parts = new ArrayList<>();
                            if (turn.getContent() != null && !turn.getContent().isBlank()) {
                                parts.add(Part.fromText(turn.getContent()));
                            }
                            for (AiToolCall tc : turn.getToolCalls()) {
                                parts.add(Part.fromFunctionCall(
                                        tc.getName(),
                                        tc.getArguments(),
                                        tc.getThoughtSignature()
                                ));
                            }
                            contents.add(Content.builder().role("model").parts(parts).build());
                        } else {
                            contents.add(Content.model(turn.getContent() != null ? turn.getContent() : ""));
                        }
                    } else {
                        // Unknown or unmapped role: default safely to user turn
                        contents.add(Content.user(turn.getContent() != null ? turn.getContent() : ""));
                    }
                }
            }
        }

        if (!pendingToolResponses.isEmpty()) {
            contents.add(Content.functionResponses(new ArrayList<>(pendingToolResponses)));
            pendingToolResponses.clear();
        }

        // Defensive normalization: Ensure every Gemini content object has strictly "user" or "model" role
        for (Content c : contents) {
            if ("function".equalsIgnoreCase(c.getRole()) || "tool".equalsIgnoreCase(c.getRole())) {
                c.setRole("user");
            } else if ("assistant".equalsIgnoreCase(c.getRole())) {
                c.setRole("model");
            } else if (!"model".equalsIgnoreCase(c.getRole())) {
                c.setRole("user");
            }
        }

        List<Tool> tools = new ArrayList<>();
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            tools.add(Tool.builder().functionDeclarations(request.getTools()).build());
        }
        if (request.isUseWebGrounding()) {
            tools.add(Tool.googleSearch());
        }

        return GeminiChatRequest.builder()
                .systemInstruction(systemInstruction)
                .contents(contents)
                .tools(tools.isEmpty() ? null : tools)
                .build();
    }
}
