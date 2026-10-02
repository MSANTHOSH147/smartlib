package com.smartlib.ai;

import com.smartlib.ai.evaluation.AiEvaluationCase;
import com.smartlib.ai.evaluation.AiEvaluationReport;
import com.smartlib.ai.evaluation.AiEvaluationRunner;
import com.smartlib.ai.tools.SmartLibToolDefinitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiEvaluationTest {

    private final AiEvaluationRunner runner = new AiEvaluationRunner();

    @Test
    @DisplayName("Evaluation dataset loads and contains required cases")
    void testEvaluationDatasetLoads() {
        List<AiEvaluationCase> cases = runner.loadCases();
        assertNotNull(cases);
        assertFalse(cases.isEmpty(), "Evaluation dataset should not be empty");
        assertTrue(cases.size() >= 25, "Expected at least 25 evaluation cases in initial dataset");
    }

    @Test
    @DisplayName("Evaluation dataset has unique IDs")
    void testEvaluationDatasetUniqueIds() {
        List<AiEvaluationCase> cases = runner.loadCases();
        Set<String> ids = new HashSet<>();
        for (AiEvaluationCase c : cases) {
            assertNotNull(c.getId(), "Evaluation case ID must not be null");
            assertTrue(ids.add(c.getId()), "Duplicate case ID found: " + c.getId());
        }
    }

    @Test
    @DisplayName("Evaluation dataset schema validity: required fields present")
    void testEvaluationDatasetSchemaValidity() {
        List<AiEvaluationCase> cases = runner.loadCases();
        for (AiEvaluationCase c : cases) {
            assertNotNull(c.getId(), "Case ID is required");
            assertNotNull(c.getCategory(), "Category is required for case: " + c.getId());
            assertNotNull(c.getQuestion(), "Question is required for case: " + c.getId());
            assertFalse(c.getQuestion().trim().isEmpty(), "Question must not be blank for case: " + c.getId());
        }
    }

    @Test
    @DisplayName("Expected tools exist in SmartLibToolDefinitions registry")
    void testExpectedToolsExistInRegistry() {
        List<AiEvaluationCase> cases = runner.loadCases();
        Set<String> registeredTools = SmartLibToolDefinitions.getAllDeclarations().stream()
                .map(com.smartlib.ai.dto.gemini.FunctionDeclaration::getName)
                .collect(java.util.stream.Collectors.toSet());

        for (AiEvaluationCase c : cases) {
            if (c.getExpectedTool() != null) {
                assertTrue(registeredTools.contains(c.getExpectedTool()),
                        "Expected tool '" + c.getExpectedTool() + "' in case " + c.getId() + " is not registered");
            }
            if (c.getExpectedTools() != null) {
                for (String tool : c.getExpectedTools()) {
                    assertTrue(registeredTools.contains(tool),
                            "Expected tool '" + tool + "' in case " + c.getId() + " is not registered");
                }
            }
        }
    }

    @Test
    @DisplayName("Security evaluation cases are present and enforce constraints")
    void testSecurityCasesPresentAndEnforced() {
        List<AiEvaluationCase> cases = runner.loadCases();
        List<AiEvaluationCase> securityCases = cases.stream()
                .filter(c -> "SECURITY".equalsIgnoreCase(c.getCategory()))
                .toList();

        assertFalse(securityCases.isEmpty(), "Security category must have evaluation cases");
        for (AiEvaluationCase sc : securityCases) {
            assertNotNull(sc.getExpectedBehavior(), "Security case " + sc.getId() + " must have expectedBehavior");
        }
    }

    @Test
    @DisplayName("AiEvaluationRunner calculates factual metrics against dataset")
    void testAiEvaluationRunnerExecution() {
        List<AiEvaluationCase> cases = runner.loadCases();
        assertNotNull(cases);
        assertFalse(cases.isEmpty());

        AiEvaluationReport report = runner.evaluate(cases);

        assertNotNull(report);
        assertEquals(25, report.getTotalCases());
        assertTrue(report.getToolSelectionAccuracy() >= 0.70, "Tool selection accuracy should be >= 70%");
        assertEquals(0, report.getSecurityViolationCount(), "Security violations must be 0");
        assertTrue(report.getSourceTypeCorrectnessRate() >= 0.70, "Source type accuracy should be >= 70%");

        String summary = report.printSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("Evaluation cases:                      25"));
        assertTrue(summary.contains("Security violations:                   0"));
    }
}
