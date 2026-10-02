package com.smartlib.ai.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * Data structure representing a standardized SmartLib AI evaluation benchmark case.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEvaluationCase {

    private String id;
    private String category;
    private String question;
    private String expectedBehavior;
    private String expectedTool;
    private List<String> expectedTools;
    private String expectedSourceType;
    private List<Long> expectedBookIds;
    private List<String> mustNotContain;
    private String notes;
}
