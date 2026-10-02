package com.smartlib.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartlib.ai.dto.gemini.Content;
import com.smartlib.exception.BadRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatRequest {

    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int MAX_HISTORY_MESSAGES = 20;

    @NotBlank(message = "Message must not be blank")
    @Size(max = MAX_MESSAGE_LENGTH, message = "Message must not exceed 1000 characters")
    private String message;

    private List<ChatMessageDto> history;

    public List<Content> toContentList() {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        if (history.size() > MAX_HISTORY_MESSAGES) {
            throw new BadRequestException("History exceeds maximum allowed limit of " + MAX_HISTORY_MESSAGES + " messages");
        }

        List<Content> contents = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            ChatMessageDto msg = history.get(i);
            if (msg == null) {
                throw new BadRequestException("History contains a null message entry at index " + i);
            }
            if (msg.getRole() == null || msg.getRole().trim().isBlank()) {
                throw new BadRequestException("History message role is required at index " + i);
            }

            String cleanRole = msg.getRole().trim().toLowerCase();
            if (!"user".equals(cleanRole) && !"model".equals(cleanRole) && !"assistant".equals(cleanRole)) {
                throw new BadRequestException("Invalid history role '" + msg.getRole() + "' at index " + i + ". Allowed roles: 'user', 'model'");
            }

            if (msg.getContent() == null || msg.getContent().trim().isBlank()) {
                throw new BadRequestException("History message content cannot be blank at index " + i);
            }

            if (msg.getContent().length() > MAX_MESSAGE_LENGTH) {
                throw new BadRequestException("History message content exceeds maximum allowed length of " + MAX_MESSAGE_LENGTH + " characters at index " + i);
            }

            if ("user".equals(cleanRole)) {
                contents.add(Content.user(msg.getContent().trim()));
            } else {
                contents.add(Content.model(msg.getContent().trim()));
            }
        }
        return contents;
    }
}
