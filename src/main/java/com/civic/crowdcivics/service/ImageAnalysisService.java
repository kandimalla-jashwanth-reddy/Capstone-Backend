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

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Base64;

@Service
public class ImageAnalysisService {

    @Value("${sightengine.api.user:58406095}")
    private String sightEngineApiUser;

    @Value("${sightengine.api.secret:pPLtxiBpgXrcF7rU7tR2pYqVRd4mb6tu}")
    private String sightEngineApiSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-1.5-flash-8b}")
    private String geminiModel;

    public Map<String, Object> analyzeImage(MultipartFile file) throws IOException {
        Map<String, Object> results = new HashMap<>();
        File tempFile = null;

        try {
            tempFile = convert(file);

            // 1. SightEngine Security Check
            Map<String, Object> securityResults = callSightEngineApi(tempFile);
            boolean isMorphed = (boolean) securityResults.getOrDefault("isMorphed", false);
            results.put("isMorphed", isMorphed);
            results.put("aiGenConfidence", securityResults.getOrDefault("aiGenConfidence", 0.0));

            // 2. Gemini Identification
            String identifiedCategory = identifyWithGemini(tempFile);
            results.put("identified_category", identifiedCategory);

            // Valid = NOT morphed AND Gemini detected a real civic issue category
            boolean isValid = !isMorphed && !identifiedCategory.equalsIgnoreCase("OTHER");
            results.put("isValid", isValid);
            results.put("verificationStatus", isValid ? "VALID" : "INVALID");

            if (!isValid) {
                if (isMorphed) {
                    results.put("rejectionReason", "Photo appears to be manipulated or AI-generated. Please upload an original photo.");
                } else {
                    results.put("rejectionReason", "No clear civic issue (pothole, garbage, streetlight, water leak) detected in the photo.");
                }
            }

        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            results.put("isValid", false);
            results.put("identified_category", "OTHER");
            results.put("isMorphed", false);
            results.put("rejectionReason", "Verification service error: " + e.getMessage());
            results.put("error", e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
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

        try {
            return analyzeImage(new CustomMultipartFile(tempFile));
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String identifyWithGemini(File file) {
        try {
            // Using the official Google Gen AI SDK (com.google.genai)
            Client client = Client.builder().apiKey(geminiApiKey).build();
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            String prompt = "Analyze this image and identify if it shows one of the following civic issues:\n" +
                    "- STREETLIGHT (broken, flickering, or damaged street lights)\n" +
                    "- POTHOLE (holes or damage in the road surface)\n" +
                    "- GARBAGE (overflowing bins, litter, or illegal dumping)\n" +
                    "- WATER (water leakage, burst pipes, or flooding)\n" +
                    "\n" +
                    "If it accurately fits one of these, return ONLY the category name in uppercase. \n" +
                    "If it doesn't clearly fit any of these, return 'OTHER'.";

            Part imagePart = Part.fromBytes(fileContent, "image/jpeg");
            Part textPart = Part.fromText(prompt);

            GenerateContentResponse response = client.models.generateContent(
                geminiModel, 
                Content.fromParts(textPart, imagePart),
                null
            );

            if (response != null && response.text() != null) {
                String text = response.text().trim().toUpperCase();
                List<String> validCategories = List.of("STREETLIGHT", "POTHOLE", "GARBAGE", "WATER");
                return validCategories.contains(text) ? text : "OTHER";
            }
        } catch (Exception e) {
            System.err.println("Gemini SDK call failed: " + e.getMessage());
        }
        return "OTHER";
    }

    private Map<String, Object> callSightEngineApi(File file) {
        Map<String, Object> securityResults = new HashMap<>();
        securityResults.put("isMorphed", false);
        securityResults.put("aiGenConfidence", 0.0); // Initialize to 0.0

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media", new FileSystemResource(file));
        body.add("models", "genai");
        body.add("api_user", sightEngineApiUser);
        body.add("api_secret", sightEngineApiSecret);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity("https://api.sightengine.com/1.0/check.json", requestEntity, JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                double aiScore = response.getBody().path("genai").path("confidence").asDouble(0.0);
                securityResults.put("isMorphed", aiScore > 0.5);
                securityResults.put("aiGenConfidence", aiScore);
            }
        } catch (Exception e) {
            System.err.println("SightEngine API call failed: " + e.getMessage());
        }
        return securityResults;
    }

    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    private static class CustomMultipartFile implements MultipartFile {
        private final File file;

        public CustomMultipartFile(File file) {
            this.file = file;
        }

        @Override public String getName() { return file.getName(); }
        @Override public String getOriginalFilename() { return file.getName(); }
        @Override public String getContentType() { return "image/jpeg"; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(file.toPath()); }
        @Override public InputStream getInputStream() throws IOException { return new FileInputStream(file); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException { Files.copy(file.toPath(), dest.toPath()); }
    }
}
