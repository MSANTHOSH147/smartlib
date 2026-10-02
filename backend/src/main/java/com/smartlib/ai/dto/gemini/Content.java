package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    private String role;
    private List<Part> parts;

    public static Content user(String text) {
        return Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(text)))
                .build();
    }

    public static Content model(String text) {
        return Content.builder()
                .role("model")
                .parts(List.of(Part.fromText(text)))
                .build();
    }

    public static Content system(String text) {
        return Content.builder()
                .parts(List.of(Part.fromText(text)))
                .build();
    }

    public static Content functionResponse(String name, Map<String, Object> response) {
        return Content.builder()
                .role("function")
                .parts(List.of(Part.fromFunctionResponse(name, response)))
                .build();
    }

    public static Content functionResponses(List<Part> parts) {
        return Content.builder()
                .role("function")
                .parts(parts)
                .build();
    }
}
