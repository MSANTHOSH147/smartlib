package com.smartlib.ai.service;

import com.smartlib.ai.dto.BookSearchResult;

import java.util.List;

/**
 * Strategy interface for re-ranking hybrid search book candidates.
 */
public interface BookReranker {

    /**
     * Reranks candidates based on query relevance using deterministic scoring.
     */
    List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates);

    /**
     * Reranks candidates and bounds the result to the specified limit.
     */
    List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates, int limit);
}
