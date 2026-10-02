package com.smartlib.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AiWebGroundingProperties {

    @Value("${ai.web-grounding.enabled:${ai.web.grounding.enabled:true}}")
    private boolean enabled = true;

    @Value("${ai.web-grounding.max-results:${ai.web.grounding.max.results:5}}")
    private int maxResults = 5;
}
