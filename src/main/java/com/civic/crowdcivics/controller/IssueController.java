package com.civic.crowdcivics.controller;

import com.civic.crowdcivics.model.Issue;
import com.civic.crowdcivics.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class IssueController {

    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private IssueRepository issueRepository;

    @GetMapping("/issues")
    public ResponseEntity<List<Issue>> getIssues(
            @RequestParam(required = false) String reporterEmail,
            @RequestParam(required = false) Issue.Status status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String assignedDepartment
    ) {
        List<Issue> issues;

        if (reporterEmail != null && !reporterEmail.isBlank()) {
            issues = issueRepository.findByReporterEmailOrderByCreatedAtDesc(reporterEmail);
        } else if (status != null) {
            issues = issueRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (category != null && !category.isBlank()) {
            issues = issueRepository.findByCategoryOrderByCreatedAtDesc(category);
        } else if (assignedDepartment != null && !assignedDepartment.isBlank()) {
            issues = issueRepository.findByAssignedDepartmentOrderByCreatedAtDesc(assignedDepartment);
        } else {
            issues = issueRepository.findAll()
                    .stream()
                    .sorted(Comparator.comparing(Issue::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(issues);
    }

    @GetMapping("/issues/{id}")
    public ResponseEntity<Issue> getIssue(@PathVariable Long id) {
        return issueRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(
            value = "/issues",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<Issue> createIssue(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam(defaultValue = "MEDIUM") Issue.Priority priority,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reporterName,
            @RequestParam(required = false) String reporterEmail,
            @RequestParam(required = false) String reporterPhone,
            @RequestPart(required = false) MultipartFile photo
    ) throws IOException {

        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setCategory(category);
        issue.setPriority(priority);
        issue.setLatitude(latitude);
        issue.setLongitude(longitude);
        issue.setAddress(address);
        issue.setReporterName(reporterName);
        issue.setReporterEmail(reporterEmail);
        issue.setReporterPhone(reporterPhone);

        // Simple routing logic based on category
        String department = routeDepartment(category);
        issue.setAssignedDepartment(department);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = savePhoto(photo);
            issue.setPhotoUrl(photoUrl);
        }

        Issue saved = issueRepository.save(issue);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/issues/{id}/status")
    public ResponseEntity<Issue> updateStatus(
            @PathVariable Long id,
            @RequestParam Issue.Status status,
            @RequestParam(required = false) String assignedTo
    ) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        if (issueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Issue issue = issueOpt.get();
        issue.setStatus(status);
        if (assignedTo != null && !assignedTo.isBlank()) {
            issue.setAssignedTo(assignedTo);
        }

        Issue updated = issueRepository.save(issue);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/issues/{id}/assign")
    public ResponseEntity<Issue> assignIssue(
            @PathVariable Long id,
            @RequestParam String department,
            @RequestParam(required = false) String assignedTo
    ) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        if (issueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Issue issue = issueOpt.get();
        issue.setAssignedDepartment(department);
        if (assignedTo != null && !assignedTo.isBlank()) {
            issue.setAssignedTo(assignedTo);
        }

        Issue updated = issueRepository.save(issue);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/admin/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        List<Issue> allIssues = issueRepository.findAll();
        summary.put("totalIssues", allIssues.size());

        Map<Issue.Status, Long> byStatus = Arrays.stream(Issue.Status.values())
                .collect(Collectors.toMap(
                        s -> s,
                        s -> allIssues.stream().filter(i -> i.getStatus() == s).count()
                ));
        summary.put("byStatus", byStatus);

        Map<String, Long> byCategory = allIssues.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Issue::getCategory,
                        Collectors.counting()
                ));
        summary.put("byCategory", byCategory);

        // Average resolution time in hours for resolved issues
        List<Duration> resolutionDurations = allIssues.stream()
                .filter(i -> i.getStatus() == Issue.Status.RESOLVED
                        && i.getCreatedAt() != null
                        && i.getResolvedAt() != null)
                .map(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()))
                .toList();

        double avgResolutionHours = 0.0;
        if (!resolutionDurations.isEmpty()) {
            long totalSeconds = resolutionDurations.stream()
                    .mapToLong(Duration::getSeconds)
                    .sum();
            avgResolutionHours = totalSeconds / 3600.0 / resolutionDurations.size();
        }
        summary.put("avgResolutionHours", avgResolutionHours);

        return ResponseEntity.ok(summary);
    }

    private String routeDepartment(String category) {
        if (category == null) {
            return "General Services";
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("pothole") || normalized.contains("road") || normalized.contains("street")) {
            return "Public Works";
        }
        if (normalized.contains("light") || normalized.contains("lamp")) {
            return "Electrical Department";
        }
        if (normalized.contains("trash") || normalized.contains("garbage") || normalized.contains("waste") || normalized.contains("bin")) {
            return "Sanitation";
        }
        if (normalized.contains("water") || normalized.contains("sewage") || normalized.contains("drain")) {
            return "Water & Sewage";
        }
        return "General Services";
    }

    private String savePhoto(MultipartFile photo) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String originalFilename = Objects.requireNonNullElse(photo.getOriginalFilename(), "photo");
        String fileExt = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex != -1) {
            fileExt = originalFilename.substring(dotIndex);
        }

        String uniqueName = UUID.randomUUID() + fileExt;
        Path target = Paths.get(UPLOAD_DIR, uniqueName);

        Files.copy(photo.getInputStream(), target);

        // This URL is exposed via WebConfig
        return "/uploads/" + uniqueName;
    }
}

