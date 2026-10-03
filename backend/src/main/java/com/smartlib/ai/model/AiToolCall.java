package com.smartlib.ai.model;

import lombok.*;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiToolCall {

    private String id;
    private String name;
    @Builder.Default
    private Map<String, Object> arguments = Collections.emptyMap();
    private String thoughtSignature;
}
