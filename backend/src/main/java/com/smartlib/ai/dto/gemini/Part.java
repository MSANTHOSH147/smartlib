package com.smartlib.ai.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {

    private String text;
    private FunctionCall functionCall;
    private FunctionResponse functionResponse;

    public static Part fromText(String text) {
        return Part.builder()
                .text(text)
                .build();
    }

    public static Part fromFunctionCall(String name, Map<String, Object> args) {
        return Part.builder()
                .functionCall(FunctionCall.builder()
                        .name(name)
                        .args(args)
                        .build())
                .build();
    }

    public static Part fromFunctionResponse(String name, Map<String, Object> response) {
        return Part.builder()
                .functionResponse(FunctionResponse.builder()
                        .name(name)
                        .response(response)
                        .build())
                .build();
    }
}
