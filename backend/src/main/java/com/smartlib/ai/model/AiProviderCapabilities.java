package com.smartlib.ai.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiProviderCapabilities {

    @Builder.Default
    private boolean supportsToolCalling = true;

    @Builder.Default
    private boolean supportsStreaming = false;

    @Builder.Default
    private boolean supportsVision = false;

    @Builder.Default
    private boolean supportsStructuredOutput = false;

    @Builder.Default
    private boolean supportsWebGrounding = false;
}
