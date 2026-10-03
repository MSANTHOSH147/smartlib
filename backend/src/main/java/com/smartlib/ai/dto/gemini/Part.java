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
    private String thoughtSignature;

    public static Part fromText(String text) {
        return Part.builder()
                .text(text)
                .build();
    }

    public static Part fromFunctionCall(String name, Map<String, Object> args) {
        return fromFunctionCall(name, args, null);
    }

    public static Part fromFunctionCall(String name, Map<String, Object> args, String thoughtSignature) {
        return Part.builder()
                .functionCall(FunctionCall.builder()
                        .name(name)
                        .args(args)
                        .build())
                .thoughtSignature(thoughtSignature)
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
