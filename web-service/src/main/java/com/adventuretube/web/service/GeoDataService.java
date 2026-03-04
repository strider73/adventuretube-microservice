package com.adventuretube.web.service;

import com.adventuretube.common.client.ServiceClient;
import com.adventuretube.common.client.ServiceClientException;
import com.adventuretube.web.exceptions.GeoServiceException;
import com.adventuretube.web.exceptions.code.WebErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoDataService {

    private static final String BASE_URL = "http://GEOSPATIAL-SERVICE";
    private final ServiceClient serviceClient;

    public JsonNode findAll() {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data",
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    public JsonNode findById(String id) {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data/" + id,
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    public JsonNode findByYoutubeContentID(String youtubeContentID) {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data/youtube/" + youtubeContentID,
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    public JsonNode findByContentType(String contentType) {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data/type/" + contentType,
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    public JsonNode findByCategory(String category) {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data/category/" + category,
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    public JsonNode count() {
        try {
            return serviceClient.getRawNonReactive(BASE_URL, "/geo/data/count",
                    new ParameterizedTypeReference<>() {});
        } catch (ServiceClientException ex) {
            throw mapServiceClientException(ex);
        }
    }

    private GeoServiceException mapServiceClientException(ServiceClientException ex) {
        log.error("Geospatial service error: {}", ex.toString());
        WebErrorCode errorCode = switch (ex.getErrorCode()) {
            case "DATA_NOT_FOUND", "USER_NOT_FOUND" -> WebErrorCode.DATA_NOT_FOUND;
            case "DUPLICATE_KEY" -> WebErrorCode.DUPLICATE_KEY;
            case "SERVER_NOT_AVAILABLE" -> WebErrorCode.SERVER_NOT_AVAILABLE;
            case "CIRCUIT_OPEN" -> WebErrorCode.SERVICE_CIRCUIT_OPEN;
            default -> WebErrorCode.INTERNAL_ERROR;
        };
        return new GeoServiceException(errorCode);
    }
}
