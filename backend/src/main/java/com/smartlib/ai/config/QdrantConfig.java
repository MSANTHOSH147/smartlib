package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class QdrantConfig {

    @Value("${qdrant.host:http://localhost:6333}")
    private String host;

    @Value("${qdrant.api.key:}")
    private String apiKey;

    @Value("${qdrant.collection.name:smartlib_books}")
    private String collectionName;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isBlank();
    }
}
