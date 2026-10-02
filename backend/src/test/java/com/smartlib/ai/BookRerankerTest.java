package com.smartlib.ai;

import com.smartlib.ai.dto.BookSearchResult;
import com.smartlib.ai.dto.PersonalizedRecommendation;
import com.smartlib.ai.dto.qdrant.ScoredPoint;
import com.smartlib.ai.service.DeterministicBookReranker;
import com.smartlib.ai.service.GeminiClient;
import com.smartlib.ai.service.HybridBookSearchService;
import com.smartlib.ai.service.PersonalizedRecommendationService;
import com.smartlib.ai.service.QdrantVectorService;
import com.smartlib.entity.Book;
import com.smartlib.entity.Category;
import com.smartlib.entity.User;
import com.smartlib.enums.Role;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookRerankerTest {

    private DeterministicBookReranker reranker;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @Mock
    private BorrowingRepository borrowingRepository;

    @BeforeEach
    void setUp() {
        reranker = new DeterministicBookReranker();
    }

    private BookSearchResult createCandidate(Long id, String title, String author, String category,
                                             Double semanticScore, Integer lexicalRank, int availableCopies) {
        return BookSearchResult.builder()
                .bookId(id)
                .title(title)
                .author(author)
                .categoryName(category)
                .semanticScore(semanticScore)
                .lexicalRank(lexicalRank)
                .availableCopies(availableCopies)
                .totalCopies(availableCopies + 1)
                .build();
    }

    @Test
    @DisplayName("RERANKER: Promotes exact title match")
    void testRerankerPromotesExactTitleMatch() {
        BookSearchResult b1 = createCandidate(1L, "Clean Code", "Robert C. Martin", "Software", 0.70, 2, 2);
        BookSearchResult b2 = createCandidate(2L, "The Clean Coder", "Robert C. Martin", "Software", 0.75, 1, 2);

        List<BookSearchResult> reranked = reranker.rerank("Clean Code", List.of(b2, b1));

        assertThat(reranked).isNotEmpty();
        assertThat(reranked.get(0).getBookId()).isEqualTo(1L);
        assertThat(reranked.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("RERANKER: Promotes author match")
    void testRerankerPromotesAuthorMatch() {
        BookSearchResult b1 = createCandidate(1L, "Refactoring", "Martin Fowler", "Software", 0.65, null, 2);
        BookSearchResult b2 = createCandidate(2L, "Code Complete", "Steve McConnell", "Software", 0.65, null, 2);

        List<BookSearchResult> reranked = reranker.rerank("Martin Fowler", List.of(b2, b1));

        assertThat(reranked).isNotEmpty();
        assertThat(reranked.get(0).getBookId()).isEqualTo(1L);
        assertThat(reranked.get(0).getAuthor()).isEqualTo("Martin Fowler");
    }

    @Test
    @DisplayName("RERANKER: Uses semantic similarity")
    void testRerankerUsesSemanticSimilarity() {
        BookSearchResult b1 = createCandidate(1L, "Book Alpha", "Author X", "Tech", 0.95, null, 2);
        BookSearchResult b2 = createCandidate(2L, "Book Beta", "Author Y", "Tech", 0.30, null, 2);

        List<BookSearchResult> reranked = reranker.rerank("microservices", List.of(b2, b1));

        assertThat(reranked).isNotEmpty();
        assertThat(reranked.get(0).getBookId()).isEqualTo(1L);
        assertThat(reranked.get(0).getRelevanceScore()).isGreaterThan(reranked.get(1).getRelevanceScore());
    }

    @Test
    @DisplayName("RERANKER: Uses category match")
    void testRerankerUsesCategoryMatch() {
        BookSearchResult b1 = createCandidate(1L, "Random Title A", "Author A", "Science Fiction", 0.60, null, 1);
        BookSearchResult b2 = createCandidate(2L, "Random Title B", "Author B", "History", 0.60, null, 1);

        List<BookSearchResult> reranked = reranker.rerank("Science Fiction", List.of(b2, b1));

        assertThat(reranked).isNotEmpty();
        assertThat(reranked.get(0).getBookId()).isEqualTo(1L);
        assertThat(reranked.get(0).getCategoryName()).isEqualTo("Science Fiction");
    }

    @Test
    @DisplayName("RERANKER: Preserves candidate limit")
    void testRerankerPreservesCandidateLimit() {
        List<BookSearchResult> list = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            list.add(createCandidate(i, "Book " + i, "Author " + i, "Tech", 0.5, (int) i, 1));
        }

        List<BookSearchResult> reranked = reranker.rerank("Tech", list, 5);

        assertThat(reranked).hasSize(5);
    }

    @Test
    @DisplayName("RERANKER: Handles empty candidates")
    void testRerankerHandlesEmptyCandidates() {
        List<BookSearchResult> reranked = reranker.rerank("Query", Collections.emptyList(), 5);
        assertThat(reranked).isEmpty();

        List<BookSearchResult> nullResult = reranker.rerank("Query", null, 5);
        assertThat(nullResult).isEmpty();
    }

    @Test
    @DisplayName("RERANKER: Handles missing signals gracefully")
    void testRerankerHandlesMissingSignals() {
        // Semantic score is null, lexical rank is null
        BookSearchResult b1 = createCandidate(1L, "Domain Driven Design", "Eric Evans", null, null, null, 2);
        BookSearchResult b2 = createCandidate(2L, "Unrelated Book", "Other Author", null, null, null, 2);

        List<BookSearchResult> reranked = reranker.rerank("Domain Driven Design", List.of(b2, b1));

        assertThat(reranked).isNotEmpty();
        assertThat(reranked.get(0).getBookId()).isEqualTo(1L);
        assertThat(reranked.get(0).getRelevanceScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("RERANKER: Is deterministic across repeated invocations")
    void testRerankerIsDeterministic() {
        BookSearchResult b1 = createCandidate(1L, "Design Patterns", "GoF", "Software", 0.8, 1, 2);
        BookSearchResult b2 = createCandidate(2L, "Patterns of Enterprise Architecture", "Martin Fowler", "Software", 0.8, 2, 2);

        List<BookSearchResult> run1 = reranker.rerank("Patterns", List.of(b1, b2));
        List<BookSearchResult> run2 = reranker.rerank("Patterns", List.of(b1, b2));

        assertThat(run1).hasSameSizeAs(run2);
        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).getBookId()).isEqualTo(run2.get(i).getBookId());
            assertThat(run1.get(i).getRelevanceScore()).isEqualTo(run2.get(i).getRelevanceScore());
        }
    }

    @Test
    @DisplayName("RERANKER: Unavailable book does not become available due to reranking")
    void testUnavailableBookDoesNotBecomeAvailable() {
        BookSearchResult unavailable = createCandidate(10L, "Out of Stock Book", "Author", "Tech", 0.99, 1, 0);

        List<BookSearchResult> reranked = reranker.rerank("Out of Stock Book", List.of(unavailable));

        assertThat(reranked).hasSize(1);
        assertThat(reranked.get(0).getAvailableCopies()).isEqualTo(0);
    }

    @Test
    @DisplayName("RERANKER: Hybrid search still uses RRF before reranking")
    void testHybridSearchStillUsesRRF() {
        HybridBookSearchService hybridService = new HybridBookSearchService(
                bookRepository, geminiClient, qdrantVectorService, new com.smartlib.ai.config.GeminiAiProperties(), reranker
        );

        Category cat = Category.builder().id(1L).name("Tech").build();
        Book book = Book.builder().id(50L).title("RRF Search Book").author("Author").category(cat).availableCopies(2).totalCopies(2).build();

        when(bookRepository.searchLexical("RRF")).thenReturn(List.of(book));
        when(geminiClient.isAvailable()).thenReturn(false); // only lexical branch
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        List<BookSearchResult> results = hybridService.search("RRF", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBookId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("RERANKER: Recommendation ranking is unaffected by search reranker")
    void testRecommendationRankingUnaffected() {
        PersonalizedRecommendationService recService = new PersonalizedRecommendationService(
                borrowingRepository, bookRepository, geminiClient, qdrantVectorService,
                new com.smartlib.ai.config.GeminiAiProperties()
        );

        User user = User.builder().id(1L).email("user@test.com").role(Role.MEMBER).build();
        Category cat = Category.builder().id(1L).name("Fiction").build();
        Book book = Book.builder().id(100L).title("1984").author("George Orwell").category(cat).availableCopies(2).averageRating(4.5).build();

        when(borrowingRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(List.of(book));

        List<PersonalizedRecommendation> recs = recService.getRecommendations(user, 5);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).getBookId()).isEqualTo(100L);
    }
}
