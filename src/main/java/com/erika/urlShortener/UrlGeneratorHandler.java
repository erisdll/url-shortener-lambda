package com.erika.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UrlGeneratorHandler implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();

        Map<String, String> bodyMap;
        try {
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing request body", e);
        }

        String originalUrl = bodyMap.get("url");
        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        String expirationTime = bodyMap.get("expirationTime");
        long expirationTimeInSeconds = Long.parseLong(expirationTime);

        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        try {
            String urlDataJson = objectMapper.writeValueAsString(urlData);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket("url-shortener-lambda-java-rocketseat-mini-course")
                    .key(shortUrlCode)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(urlDataJson));
        } catch (Exception exception) {
            throw new RuntimeException("Error saving URL data to S3 Bucket" + exception.getMessage(), exception);
        }

        Map<String, String> response = new HashMap<>();
        response.put("shortUrl", shortUrlCode);

        return response;
    }
}