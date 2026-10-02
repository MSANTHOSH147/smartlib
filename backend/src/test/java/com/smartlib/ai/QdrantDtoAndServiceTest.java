package com.smartlib.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.config.QdrantConfig;
import com.smartlib.ai.dto.qdrant.*;
import com.smartlib.ai.service.QdrantVectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class QdrantDtoAndServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("CreateCollectionRequest serializes with 768 dimensions and Cosine distance")
    void testCreateCollectionSerialization() throws Exception {
        CreateCollectionRequest request = CreateCollectionRequest.of(768, "Cosine");

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"size\":768"));
        assertTrue(json.contains("\"distance\":\"Cosine\""));
    }

    @Test
    @DisplayName("CollectionInfo extracts vector size and distance correctly")
    void testCollectionInfoDeserialization() throws Exception {
        String json = """
        {
          "status": "green",
          "points_count": 42,
          "config": {
            "params": {
              "vectors": {
                "size": 768,
                "distance": "Cosine"
              }
            }
          }
        }
        """;

        CollectionInfo info = objectMapper.readValue(json, CollectionInfo.class);

        assertNotNull(info);
        assertEquals(768, info.getVectorSize());
        assertEquals("Cosine", info.getVectorDistance());
        assertEquals(42L, info.getPointsCount());
    }

    @Test
    @DisplayName("PointStruct and UpsertPointsRequest serialize properly")
    void testUpsertRequestSerialization() throws Exception {
        PointStruct point = PointStruct.of(
                101,
                List.of(0.1f, 0.2f, 0.3f),
                Map.of("bookId", 101, "title", "Clean Code")
        );
        UpsertPointsRequest request = UpsertPointsRequest.of(List.of(point));

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"points\""));
        assertTrue(json.contains("\"id\":101"));
        assertTrue(json.contains("\"title\":\"Clean Code\""));
    }

    @Test
    @DisplayName("SearchPointsRequest and ScoredPoint serialize and deserialize properly")
    void testSearchPointsRoundTrip() throws Exception {
        SearchPointsRequest request = SearchPointsRequest.of(List.of(0.1f, 0.2f), 5);
        String requestJson = objectMapper.writeValueAsString(request);
        assertTrue(requestJson.contains("\"limit\":5"));
        assertTrue(requestJson.contains("\"with_payload\":true"));

        String responseJson = """
        {
          "result": [
            {
              "id": 101,
              "score": 0.945,
              "payload": {
                "title": "Clean Code",
                "author": "Robert C. Martin"
              }
            }
          ],
          "status": "ok",
          "time": 0.002
        }
        """;

        QdrantApiResponse<List<ScoredPoint>> response = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(
                        QdrantApiResponse.class,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ScoredPoint.class)
                )
        );

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(1, response.getResult().size());
        assertEquals(0.945, response.getResult().get(0).getScore(), 0.001);
        assertEquals("Clean Code", response.getResult().get(0).getPayload().get("title"));
    }

    @Test
    @DisplayName("QdrantVectorService validates matching 768 dimension on existing collection")
    void testEnsureCollectionWithExistingMatchingDimension() {
        QdrantConfig qdrantConfig = new QdrantConfig();
        qdrantConfig.setHost("http://localhost:6333");
        qdrantConfig.setCollectionName("smartlib_books");

        GeminiAiProperties geminiProps = new GeminiAiProperties();
        geminiProps.setEmbeddingDimension(768);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:6333");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        QdrantVectorService service = new QdrantVectorService(restClient, qdrantConfig, geminiProps);

        String collectionInfoJson = """
        {
          "result": {
            "status": "green",
            "points_count": 10,
            "config": {
              "params": {
                "vectors": {
                  "size": 768,
                  "distance": "Cosine"
                }
              }
            }
          },
          "status": "ok"
        }
        """;

        // First call: collectionExists() -> GET /collections/smartlib_books
        server.expect(requestTo("http://localhost:6333/collections/smartlib_books"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(collectionInfoJson, MediaType.APPLICATION_JSON));

        // Second call: getCollectionInfo() -> GET /collections/smartlib_books
        server.expect(requestTo("http://localhost:6333/collections/smartlib_books"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(collectionInfoJson, MediaType.APPLICATION_JSON));

        boolean verified = service.ensureCollection();
        assertTrue(verified);
        server.verify();
    }

    @Test
    @DisplayName("QdrantVectorService creates collection if not existing")
    void testEnsureCollectionCreatesWhenNotFound() {
        QdrantConfig qdrantConfig = new QdrantConfig();
        qdrantConfig.setHost("http://localhost:6333");
        qdrantConfig.setCollectionName("smartlib_books");

        GeminiAiProperties geminiProps = new GeminiAiProperties();
        geminiProps.setEmbeddingDimension(768);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:6333");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        QdrantVectorService service = new QdrantVectorService(restClient, qdrantConfig, geminiProps);

        // collectionExists() -> returns 404 Not Found
        server.expect(requestTo("http://localhost:6333/collections/smartlib_books"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        // createCollection -> PUT /collections/smartlib_books
        server.expect(requestTo("http://localhost:6333/collections/smartlib_books"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"result\": true, \"status\": \"ok\"}", MediaType.APPLICATION_JSON));

        boolean created = service.ensureCollection();
        assertTrue(created);
        server.verify();
    }
}
