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
import java.util.Base64;
import java.io.FileOutputStream;
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
            File tempFile = convert(file);
            Map<String, Object> yoloResults = callYoloApi(tempFile);
            
            String identifiedCategory = (String) yoloResults.getOrDefault("identified_category", "OTHER");
            results.put("identified_category", identifiedCategory);
            results.put("detections", yoloResults.get("detections"));

            // 2. SightEngine Security Checks (Morphed & Camera Source)
            Map<String, Object> securityResults = callSightEngineApi(tempFile);
            results.putAll(securityResults);

            // Cleanup temp file
            if (tempFile.exists()) { tempFile.delete(); }

            // Final validity check: Must have identified category AND be a valid camera photo (not morphed)
            boolean isValid = !identifiedCategory.equals("OTHER") 
                && !(boolean)results.getOrDefault("isMorphed", false)
                && (boolean)results.getOrDefault("isCameraSource", true);
            
            results.put("isValid", isValid);
            results.put("verificationStatus", isValid ? "VALID" : "INVALID");

        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            results.put("isValid", false);
            results.put("identified_category", "OTHER");
            results.put("error", e.getMessage());
        }

        return results;
    }

    public Map<String, Object> analyzeBase64Image(String base64Image) throws IOException {
        String base64Content;
        if (base64Image.contains(",")) {
            base64Content = base64Image.split(",")[1];
        } else {
            base64Content = base64Image;
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        File tempFile = File.createTempFile("upload_", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(decodedBytes);
        }

        Map<String, Object> results = new HashMap<>();
        try {
            Map<String, Object> yoloResults = callYoloApi(tempFile);
            String identifiedCategory = (String) yoloResults.getOrDefault("identified_category", "OTHER");
            results.put("identified_category", identifiedCategory);
            results.put("isValid", true);
            results.put("verificationStatus", "VALID");
            results.put("detections", yoloResults.get("detections"));
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        return results;
    }

    private Map<String, Object> callSightEngineApi(File file) {
        Map<String, Object> securityResults = new HashMap<>();
        // Default values
        securityResults.put("isMorphed", false);
        securityResults.put("isCameraSource", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media", new FileSystemResource(file));
        body.add("models", "genai,properties");
        body.add("api_user", sightEngineApiUser);
        body.add("api_secret", sightEngineApiSecret);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity("https://api.sightengine.com/1.0/check.json", requestEntity, JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = response.getBody();
                
                // 1. AI Generated Check
                double aiScore = root.path("genai").path("confidence").asDouble(0.0);
                boolean isMorphed = aiScore > 0.5;
                securityResults.put("isMorphed", isMorphed);
                securityResults.put("aiGenConfidence", aiScore);

                // 3. Camera Source Check (Taken by camera vs Google/Web)
                // If it has camera metadata (make/model), we treat it as camera source.
                // Web/Google images usually lack these or have "software" signatures.
                JsonNode props = root.path("properties");
                boolean hasExif = !props.path("exif").isMissingNode() && props.path("exif").has("Make");
                securityResults.put("isCameraSource", hasExif);
            }
        } catch (Exception e) {
            System.err.println("SightEngine API call failed: " + e.getMessage());
            // Fail safe: assume valid if API is down, or strict: assume invalid.
            // Keeping default (true) for now but logging the error.
        }
        return securityResults;
    }

    private Map<String, Object> callYoloApi(File file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(file));

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
