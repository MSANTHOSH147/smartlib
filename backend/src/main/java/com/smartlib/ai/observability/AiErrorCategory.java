package com.smartlib.ai.observability;

public enum AiErrorCategory {
    NONE,
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    RATE_LIMIT,
    PROVIDER,
    TOOL,
    DATABASE,
    VECTOR_DB,
    WEB_GROUNDING,
    INTERNAL
}
