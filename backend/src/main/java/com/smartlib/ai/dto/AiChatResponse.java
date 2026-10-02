package com.smartlib.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartlib.ai.model.AiSource;
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
public class AiChatResponse {

    private String reply;
    private boolean success;

    @Builder.Default
    private List<String> toolsExecuted = new ArrayList<>();

    @Builder.Default
    private List<AiSource> sources = new ArrayList<>();

    public static AiChatResponse of(String reply, boolean success, List<String> toolsExecuted) {
        return of(reply, success, toolsExecuted, Collections.emptyList());
    }

    public static AiChatResponse of(String reply, boolean success, List<String> toolsExecuted, List<AiSource> sources) {
        return AiChatResponse.builder()
                .reply(reply)
                .success(success)
                .toolsExecuted(toolsExecuted != null ? toolsExecuted : new ArrayList<>())
                .sources(sources != null ? sources : new ArrayList<>())
                .build();
    }
}
