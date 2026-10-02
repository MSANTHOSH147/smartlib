package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tool {

    private List<FunctionDeclaration> functionDeclarations;
    private java.util.Map<String, Object> googleSearch;

    public static Tool of(List<FunctionDeclaration> declarations) {
        return Tool.builder()
                .functionDeclarations(declarations)
                .build();
    }

    public static Tool googleSearch() {
        return Tool.builder()
                .googleSearch(java.util.Collections.emptyMap())
                .build();
    }
}
