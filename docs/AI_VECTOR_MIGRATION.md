# SmartLib AI — Vector Database & Embedding Migration Guide

## 1. Current Embedding & Vector Specification
- **Embedding Model**: `gemini-embedding-2`
- **Vector Dimension**: `768`
- **Distance Metric**: `Cosine`
- **Target Collection**: `smartlib_books`
- **Storage Engine**: Qdrant Vector Database
- **Indexing Service**: `BookVectorIndexingService`

---

## 2. Why Embeddings Cannot Be Mixed
Embeddings represent high-dimensional geometric coordinates within a continuous semantic vector space. The geometric orientation, relative distances, and dimensional projections are unique to the exact embedding model version and training checkpoint.

- **Different Models**: Two models (e.g., OpenAI `text-embedding-3-small` vs. Google `gemini-embedding-2`) project identical text phrases to completely divergent mathematical coordinates. Calculating cosine similarity across models yields pure mathematical noise.
- **Different Dimensions**: A 768-dimensional vector cannot be compared against a 1536-dimensional or 512-dimensional vector. Qdrant will reject vectors that do not match the collection's configured dimension.
- **Model Checkpoint Changes**: If Google updates `gemini-embedding-2` to a new major architecture, distances between new vectors and legacy vectors may drift significantly.

---

## 3. Re-indexing Procedure
SmartLib incorporates automated SHA-256 content hashing in `BookVectorIndexingService` to detect catalog changes and avoid redundant API calls:
```text
semanticText = "Title: " + title + "\nAuthor: " + author + "\nCategory: " + category + "\nDescription: " + description;
contentHash = SHA-256(semanticText);
```

### Procedure to Execute Full Catalog Re-Index:
1. **Administrative Trigger**:
   An authorized library administrator can invoke the batch indexing routine through the admin service:
   ```java
   bookVectorIndexingService.indexAllBooks();
   ```
2. **Incremental Upsert**:
   The service queries all books from the authoritative MySQL database, computes the SHA-256 hash, and compares it against Qdrant point metadata (`content_hash`). If the hash differs or point does not exist, a new embedding is generated and upserted.
3. **Zero-Downtime Blue-Green Migration (For Major Model Upgrades)**:
   When transitioning to a new model or new dimension (e.g., 1536 dimensions):
   - Create a new collection (e.g. `smartlib_books_v2`) with the new dimensional specification.
   - Run batch indexing into `smartlib_books_v2`.
   - Update `application.properties` or environment variable `QDRANT_COLLECTION=smartlib_books_v2`.
   - Restart or hot-reload backend services.
   - Verify retrieval quality and decommission `smartlib_books_v1`.

---

## 4. Rollback Considerations
- **Point Retention**: Decommissioned collections should be retained for at least 72 hours before deletion.
- **Instant Rollback**: If retrieval anomalies occur post-migration, reverting `QDRANT_COLLECTION` to the previous collection name restores previous retrieval behavior instantly.
- **Snapshot Backups**: Take a Qdrant collection snapshot before triggering full re-indexing operations:
  ```bash
  curl -X POST "http://localhost:6333/collections/smartlib_books/snapshots"
  ```
