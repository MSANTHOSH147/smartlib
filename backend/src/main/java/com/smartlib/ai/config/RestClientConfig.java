package com.smartlib.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final GeminiAiProperties geminiAiProperties;
    private final QdrantConfig qdrantConfig;

    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutSeconds = geminiAiProperties.getTimeoutSeconds() > 0 ? geminiAiProperties.getTimeoutSeconds() : 30;
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public RestClient qdrantRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(qdrantConfig.getHost())
                .requestFactory(requestFactory);

        if (qdrantConfig.hasApiKey()) {
            builder.defaultHeader("api-key", qdrantConfig.getApiKey().trim());
        }

        return builder.build();
    }

    @Bean
    public RestClient groqRestClient(GroqAiProperties groqAiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutSeconds = groqAiProperties.getTimeoutSeconds() > 0 ? groqAiProperties.getTimeoutSeconds() : 30;
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        String baseUrl = groqAiProperties.getBaseUrl() != null && !groqAiProperties.getBaseUrl().isBlank()
                ? groqAiProperties.getBaseUrl()
                : "https://api.groq.com/openai/v1";

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
