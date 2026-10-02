package com.smartlib.ai.service;

import com.smartlib.ai.config.GeminiAiProperties;
import com.smartlib.ai.config.QdrantConfig;
import com.smartlib.ai.dto.qdrant.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantVectorService {

    private final RestClient qdrantRestClient;
    private final QdrantConfig qdrantConfig;
    private final GeminiAiProperties geminiAiProperties;

    public boolean isAvailable() {
        try {
            qdrantRestClient.get()
                    .uri("/")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.debug("Qdrant health check failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean collectionExists() {
        String collectionName = qdrantConfig.getCollectionName();
        try {
            QdrantApiResponse<CollectionInfo> response = qdrantRestClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<CollectionInfo>>() {});

            return response != null && response.getResult() != null;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.warn("Error checking Qdrant collection '{}': {}", collectionName, ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Failed to connect to Qdrant at '{}': {}", qdrantConfig.getHost(), ex.getMessage());
            return false;
        }
    }

    public CollectionInfo getCollectionInfo() {
        String collectionName = qdrantConfig.getCollectionName();
        try {
            QdrantApiResponse<CollectionInfo> response = qdrantRestClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<CollectionInfo>>() {});

            return response != null ? response.getResult() : null;
        } catch (Exception ex) {
            log.warn("Failed to retrieve collection info for '{}': {}", collectionName, ex.getMessage());
            return null;
        }
    }

    public synchronized boolean ensureCollection() {
        String collectionName = qdrantConfig.getCollectionName();
        int expectedDimension = geminiAiProperties.getEmbeddingDimension();

        if (collectionExists()) {
            CollectionInfo info = getCollectionInfo();
            if (info != null && info.getVectorSize() != null) {
                if (info.getVectorSize() != expectedDimension) {
                    log.error("Qdrant collection '{}' dimension mismatch! Expected: {}, Found in Qdrant: {}",
                            collectionName, expectedDimension, info.getVectorSize());
                    return false;
                }
                if (!"Cosine".equalsIgnoreCase(info.getVectorDistance())) {
                    log.warn("Qdrant collection '{}' distance metric is {}, expected Cosine.",
                            collectionName, info.getVectorDistance());
                }
            }
            log.info("Qdrant collection '{}' verified (dimension: {}, metric: Cosine).",
                    collectionName, expectedDimension);
            return true;
        }

        log.info("Qdrant collection '{}' does not exist. Creating with dimension {} and Cosine distance...",
                collectionName, expectedDimension);

        CreateCollectionRequest createRequest = CreateCollectionRequest.of(expectedDimension, "Cosine");

        try {
            QdrantApiResponse<Boolean> response = qdrantRestClient.put()
                    .uri("/collections/{name}", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<Boolean>>() {});

            boolean success = response != null && (Boolean.TRUE.equals(response.getResult()) || response.isOk());
            if (success) {
                log.info("Qdrant collection '{}' created successfully.", collectionName);
            }
            return success;
        } catch (Exception ex) {
            log.error("Failed to create Qdrant collection '{}': {}", collectionName, ex.getMessage());
            return false;
        }
    }

    public boolean upsertPoints(List<PointStruct> points) {
        if (points == null || points.isEmpty()) {
            return true;
        }

        String collectionName = qdrantConfig.getCollectionName();
        UpsertPointsRequest request = UpsertPointsRequest.of(points);

        try {
            QdrantApiResponse<Object> response = qdrantRestClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{name}/points")
                            .queryParam("wait", "true")
                            .build(collectionName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<Object>>() {});

            return response != null && response.isOk();
        } catch (Exception ex) {
            log.error("Failed to upsert {} points into Qdrant collection '{}': {}",
                    points.size(), collectionName, ex.getMessage());
            return false;
        }
    }

    public List<ScoredPoint> search(List<Float> vector, int limit) {
        if (vector == null || vector.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        String collectionName = qdrantConfig.getCollectionName();
        SearchPointsRequest request = SearchPointsRequest.of(vector, limit);

        try {
            QdrantApiResponse<List<ScoredPoint>> response = qdrantRestClient.post()
                    .uri("/collections/{name}/points/search", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<List<ScoredPoint>>>() {});

            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("Failed to search Qdrant collection '{}': {}", collectionName, ex.getMessage());
            return Collections.emptyList();
        }
    }

    public PointStruct getPoint(Long pointId) {
        if (pointId == null) {
            return null;
        }

        String collectionName = qdrantConfig.getCollectionName();
        try {
            QdrantApiResponse<PointStruct> response = qdrantRestClient.get()
                    .uri("/collections/{name}/points/{id}", collectionName, pointId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<PointStruct>>() {});

            return response != null ? response.getResult() : null;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            log.warn("Error fetching point id={} from Qdrant: {}", pointId, ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Failed to retrieve point id={} from Qdrant: {}", pointId, ex.getMessage());
            return null;
        }
    }

    public boolean deletePoint(Long pointId) {
        if (pointId == null) {
            return false;
        }

        String collectionName = qdrantConfig.getCollectionName();
        DeletePointsRequest request = DeletePointsRequest.of(pointId);

        try {
            QdrantApiResponse<Object> response = qdrantRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{name}/points/delete")
                            .queryParam("wait", "true")
                            .build(collectionName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<QdrantApiResponse<Object>>() {});

            return response != null && response.isOk();
        } catch (Exception ex) {
            log.error("Failed to delete point id={} from Qdrant collection '{}': {}",
                    pointId, collectionName, ex.getMessage());
            return false;
        }
    }
}
