package com.smartlib.ai.service;

import com.smartlib.ai.dto.BookSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Deterministic implementation of BookReranker.
 *
 * Scoring Formula:
 *   rerankScore =
 *       semanticScore * 0.40
 *     + lexicalScore  * 0.25
 *     + titleMatch    * 0.15
 *     + authorMatch   * 0.10
 *     + categoryMatch * 0.10
 *
 * All inputs are normalized to 0.0 - 1.0. If signals are missing, weights are gracefully normalized.
 * Preserves catalog availability and does not overwrite inventory metadata.
 */
@Service
@Slf4j
public class DeterministicBookReranker implements BookReranker {

    private static final double WEIGHT_SEMANTIC = 0.40;
    private static final double WEIGHT_LEXICAL  = 0.25;
    private static final double WEIGHT_TITLE    = 0.15;
    private static final double WEIGHT_AUTHOR   = 0.10;
    private static final double WEIGHT_CATEGORY = 0.10;

    @Override
    public List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates) {
        return rerank(query, candidates, candidates != null ? candidates.size() : 0);
    }

    @Override
    public List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        int targetLimit = limit > 0 ? limit : candidates.size();

        if (query == null || query.trim().isBlank()) {
            return candidates.stream().limit(targetLimit).toList();
        }

        String cleanQuery = query.trim().toLowerCase(Locale.ROOT);
        String[] queryTokens = cleanQuery.split("\\s+");

        List<ScoredCandidate> scoredList = new ArrayList<>(candidates.size());

        for (BookSearchResult candidate : candidates) {
            double titleScore = computeTitleMatch(candidate.getTitle(), cleanQuery, queryTokens);
            double authorScore = computeAuthorMatch(candidate.getAuthor(), cleanQuery, queryTokens);
            double categoryScore = computeCategoryMatch(candidate.getCategoryName(), cleanQuery, queryTokens);
            Double semanticScore = normalizeSemanticScore(candidate.getSemanticScore());
            Double lexicalScore = normalizeLexicalScore(candidate.getLexicalRank(), candidate.getRelevanceScore());

            double totalWeight = 0.0;
            double weightedSum = 0.0;

            if (semanticScore != null) {
                weightedSum += semanticScore * WEIGHT_SEMANTIC;
                totalWeight += WEIGHT_SEMANTIC;
            }

            if (lexicalScore != null) {
                weightedSum += lexicalScore * WEIGHT_LEXICAL;
                totalWeight += WEIGHT_LEXICAL;
            }

            weightedSum += titleScore * WEIGHT_TITLE;
            totalWeight += WEIGHT_TITLE;

            weightedSum += authorScore * WEIGHT_AUTHOR;
            totalWeight += WEIGHT_AUTHOR;

            weightedSum += categoryScore * WEIGHT_CATEGORY;
            totalWeight += WEIGHT_CATEGORY;

            double finalScore = totalWeight > 0.0 ? (weightedSum / totalWeight) : 0.0;
            double boundedScore = Math.max(0.0, Math.min(1.0, Math.round(finalScore * 1000.0) / 1000.0));

            candidate.setRelevanceScore(boundedScore);
            scoredList.add(new ScoredCandidate(candidate, boundedScore));
        }

        // Sort descending by rerank score, with tie-break on bookId for determinism
        return scoredList.stream()
                .sorted((a, b) -> {
                    int scoreCmp = Double.compare(b.score, a.score);
                    if (scoreCmp != 0) return scoreCmp;
                    Long idA = a.candidate.getBookId() != null ? a.candidate.getBookId() : 0L;
                    Long idB = b.candidate.getBookId() != null ? b.candidate.getBookId() : 0L;
                    return idA.compareTo(idB);
                })
                .map(sc -> sc.candidate)
                .limit(targetLimit)
                .toList();
    }

    private double computeTitleMatch(String title, String fullQuery, String[] tokens) {
        if (title == null || title.isBlank()) return 0.0;
        String t = title.toLowerCase(Locale.ROOT).trim();
        if (t.equals(fullQuery)) return 1.0;
        if (t.startsWith(fullQuery + " ") || t.endsWith(" " + fullQuery) || t.contains(" " + fullQuery + " ")) return 0.85;

        Set<String> titleWords = new HashSet<>(Arrays.asList(t.split("[\\s\\p{Punct}]+")));
        int matches = 0;
        for (String token : tokens) {
            if (!token.isBlank() && titleWords.contains(token)) {
                matches++;
            }
        }
        return tokens.length > 0 ? ((double) matches / tokens.length) * 0.40 : 0.0;
    }

    private double computeAuthorMatch(String author, String fullQuery, String[] tokens) {
        if (author == null || author.isBlank()) return 0.0;
        String a = author.toLowerCase(Locale.ROOT).trim();
        if (a.equals(fullQuery)) return 1.0;
        if (a.startsWith(fullQuery + " ") || a.endsWith(" " + fullQuery) || a.contains(" " + fullQuery + " ") || fullQuery.contains(a)) return 0.90;

        Set<String> authorWords = new HashSet<>(Arrays.asList(a.split("[\\s\\p{Punct}]+")));
        int matches = 0;
        for (String token : tokens) {
            if (!token.isBlank() && authorWords.contains(token)) {
                matches++;
            }
        }
        return tokens.length > 0 ? ((double) matches / tokens.length) * 0.50 : 0.0;
    }

    private double computeCategoryMatch(String category, String fullQuery, String[] tokens) {
        if (category == null || category.isBlank()) return 0.0;
        String c = category.toLowerCase(Locale.ROOT).trim();
        if (c.equals(fullQuery) || fullQuery.contains(c)) return 1.0;

        Set<String> categoryWords = new HashSet<>(Arrays.asList(c.split("[\\s\\p{Punct}]+")));
        for (String token : tokens) {
            if (!token.isBlank() && categoryWords.contains(token)) {
                return 0.80;
            }
        }
        return 0.0;
    }

    private Double normalizeSemanticScore(Double semanticScore) {
        if (semanticScore == null) return null;
        return Math.max(0.0, Math.min(1.0, semanticScore));
    }

    private Double normalizeLexicalScore(Integer lexicalRank, Double fallbackScore) {
        if (lexicalRank != null && lexicalRank >= 0) {
            // Rank 0 => 1.0, Rank 1 => 0.5, Rank 2 => 0.33, etc.
            return 1.0 / (1.0 + lexicalRank);
        }
        if (fallbackScore != null) {
            return Math.max(0.0, Math.min(1.0, fallbackScore));
        }
        return null;
    }

    private record ScoredCandidate(BookSearchResult candidate, double score) {}
}
