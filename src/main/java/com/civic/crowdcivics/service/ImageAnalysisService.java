package com.civic.crowdcivics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageAnalysisService {

    @Value("${sightengine.api.user:58406095}")
    private String sightEngineApiUser;

    @Value("${sightengine.api.secret:pPLtxiBpgXrcF7rU7tR2pYqVRd4mb6tu}")
    private String sightEngineApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String YOLO_API_URL = "http://localhost:5000/analyze";

    public Map<String, Object> analyzeImage(MultipartFile file) throws IOException {
        Map<String, Object> results = new HashMap<>();

        try {
            // 1. Local YOLO Detection (Fast and Offline)
            Map<String, Object> yoloResults = callYoloApi(file);

            String identifiedCategory = (String) yoloResults.getOrDefault("identified_category", "OTHER");
            results.put("identified_category", identifiedCategory);
            results.put("isValid", true); // Default to valid since we removed external AI detection
            results.put("verificationStatus", "VALID");
            results.put("detections", yoloResults.get("detections"));

        } catch (Exception e) {
            System.err.println("YOLO Analysis failed: " + e.getMessage());
            results.put("isValid", true);
            results.put("identified_category", "OTHER");
        }

        return results;
    }

    private Map<String, Object> callYoloApi(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(convert(file)));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(YOLO_API_URL, requestEntity, JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = response.getBody();
                Map<String, Object> res = new HashMap<>();
                res.put("identified_category", root.path("identified_category").asText("OTHER"));
                res.put("detections", root.path("detections"));
                return res;
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to Python YOLO API at " + YOLO_API_URL + ". Is the server running?");
            throw e;
        }
        return new HashMap<>();
    }

    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }
}
