# SmartLib AI Reranking Architecture (Phase 11)

## 1. Objective & Retrieval Pipeline
SmartLib AI employs a multi-stage retrieval architecture to maximize precision and recall in book discovery. In Phase 3, Reciprocal Rank Fusion (RRF) blended MySQL full-text keyword retrieval and Qdrant 768-dimensional semantic embeddings. Phase 11 introduces a high-precision deterministic reranking stage between RRF and MySQL entity hydration.

### Two-Stage Retrieval Architecture
```
User Query
   │
   ├───────────────────────────────┐
   ▼                               ▼
MySQL Lexical Search        Qdrant Semantic Search
(Catalog Keywords)          (Gemini Embeddings)
   │                               │
   └───────────────┬───────────────┘
                   ▼
       Reciprocal Rank Fusion (RRF)
                   ▼
         Top 20 Candidate Books
                   ▼
         Deterministic Reranker
                   ▼
          Top 5 Hydrated Books
```

---

## 2. Reranker Abstraction
The reranking layer is defined by the `BookReranker` interface:
```java
public interface BookReranker {
    List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates);
    List<BookSearchResult> rerank(String query, List<BookSearchResult> candidates, int limit);
}
```

The default engine is `DeterministicBookReranker`, providing transparent, explainable, and zero-latency reranking without incurring the cost, latency, or nondeterminism of external cross-encoder models.

---

## 3. Scoring Formula
The reranking score combines 5 complementary signals:

$$\text{rerankScore} = (\text{semanticScore} \times 0.40) + (\text{lexicalScore} \times 0.25) + (\text{titleMatch} \times 0.15) + (\text{authorMatch} \times 0.10) + (\text{categoryMatch} \times 0.10)$$

### Signal Weights and Definitions:
1. **Semantic Similarity (40%)**:
   - Cosine similarity between user query embedding and Qdrant book vector representation (normalized to `[0.0, 1.0]`).
2. **Lexical Relevance (25%)**:
   - Rank-decayed keyword score based on MySQL lexical search position: $\frac{1.0}{1.0 + \text{lexicalRank}}$.
3. **Exact & Phrase Title Match (15%)**:
   - `1.0` for exact title match (case-insensitive).
   - `0.85` for full substring match.
   - Partial token overlap weighted proportionally: $\frac{\text{matchingTokens}}{\text{totalQueryTokens}} \times 0.70$.
4. **Author Match (10%)**:
   - `1.0` if the author name matches or is contained in the query.
   - Proportional token overlap for multi-part author names.
5. **Category Match (10%)**:
   - `1.0` if the book's category matches or is contained in the query.

### Missing Signal Graceful Normalization:
If any signal is unavailable (e.g. semantic search offline, lexical match absent), the reranker dynamically reweights remaining signals by dividing by the active weights sum:

$$\text{finalScore} = \frac{\sum_{i \in \text{available}} w_i \cdot s_i}{\sum_{i \in \text{available}} w_i}$$

---

## 4. Preservation of Availability Guarantees
- **Availability is Authoritative Metadata, Not Relevance**: Reranking sorts candidate relevance; it never modifies physical copy counts (`availableCopies`).
- An unavailable book (`availableCopies == 0`) retains its out-of-stock state regardless of whether it ranks #1 in semantic and lexical relevance.

---

## 5. Separation from Recommendations
- **Hybrid Search Reranker**: Scores query-to-book relevance.
- **Personalized Recommendations**: Operates independently using circulation history, user memory preferences, collaborative similarity, and shelf availability:
  $$\text{recommendationScore} = (\text{baseScore} \times 0.85) + (\text{memoryScore} \times 0.15)$$
- Search reranking does not alter or contaminate recommendation scoring.
