package com.smartlib.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSource {

    private String type;     // "WEB", "SMARTLIB"
    private String title;
    private String url;
    private String domain;
    private String snippet;
    private String sourceId;

    public static AiSource web(String title, String url, String domain) {
        return AiSource.builder()
                .type("WEB")
                .title(title)
                .url(url)
                .domain(domain)
                .build();
    }

    public static AiSource smartlib(String title, String url, String snippet) {
        return AiSource.builder()
                .type("SMARTLIB")
                .title(title)
                .url(url)
                .snippet(snippet)
                .build();
    }
}
