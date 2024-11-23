package com.erika.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UrlRedirectHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final S3Client s3Client = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        String pathParameters = (String) input.get("rawPath");
        String shortUrlCode = pathParameters.replace("/", "");

        if (shortUrlCode.length() != 8) {
            throw new IllegalArgumentException("Invalid short URL code");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("url-shortener-lambda-java-rocketseat-mini-course")
                .key(shortUrlCode)
                .build();

        InputStream s3ObjectStream;
        try {
            s3ObjectStream = s3Client.getObject(getObjectRequest);
        } catch (Exception exception) {
            throw new RuntimeException("Error getting URL data from S3 Bucket" + exception.getMessage(), exception);
        }

        UrlData urlData;
        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch (Exception exception) {
            throw new RuntimeException("Error parsing URL data from S3 Bucket" + exception.getMessage(), exception);
        }

        Map<String, Object> response = new HashMap<>();

        long currentTimeMillis = System.currentTimeMillis();
        long expirationTimeMillis = urlData.getExpirationTime() * 1000;

        if (expirationTimeMillis < currentTimeMillis) {
            response.put("statusCode", 302);
            response.put("headers", Map.of("Location", urlData.getOriginalUrl()));
        } else {
            response.put("statusCode", 410);
            response.put("body", "This URL has expired");
}

return response;
    }
}
