package com.civic.crowdcivics.controller;

import com.civic.crowdcivics.model.Issue;
import com.civic.crowdcivics.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*")
public class IssueController {

    @Autowired
    private IssueService issueService;

    @PostMapping
    public ResponseEntity<?> reportIssue(@RequestBody Issue issue) {
        try {
            if (issue.getTitle() == null || issue.getTitle().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            if (issue.getCategory() == null || issue.getCategory().isEmpty()) {
                return ResponseEntity.badRequest().body("Category is required");
            }

            Issue createdIssue = issueService.createIssue(issue);
            return ResponseEntity.ok(createdIssue);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating issue: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Issue> getAllIssues() {
        return issueService.getAllIssues();
    }

    @GetMapping("/user/{userId}")
    public List<Issue> getUserIssues(@PathVariable Long userId) {
        return issueService.getIssuesByReporter(userId);
    }

    @GetMapping("/department/{department}")
    public List<Issue> getIssuesByDepartment(@PathVariable String department) {
        return issueService.getIssuesByDepartment(department);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIssueById(@PathVariable Long id) {
        Optional<Issue> issue = issueService.getIssueById(id);
        if (issue.isPresent()) {
            return ResponseEntity.ok(issue.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String statusStr = payload.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body("Status is required");
        }

        try {
            Issue.Status status = Issue.Status.valueOf(statusStr.toUpperCase());
            String resolutionPhotoUrl = payload.get("resolutionPhotoUrl");
            String rejectionReason = payload.get("rejectionReason");
            Issue updatedIssue = issueService.updateIssueStatus(id, status, resolutionPhotoUrl, rejectionReason);
            if (updatedIssue != null) {
                return ResponseEntity.ok(updatedIssue);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status value");
        }
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String feedback = (String) payload.get("feedback");
        Integer rating = null;
        if (payload.get("rating") != null) {
            if (payload.get("rating") instanceof Integer) {
                rating = (Integer) payload.get("rating");
            } else {
                try {
                    rating = Integer.parseInt(payload.get("rating").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("Rating must be an integer");
                }
            }
        }

        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("Valid rating between 1 and 5 is required");
        }

        Issue updatedIssue = issueService.submitFeedback(id, feedback, rating);
        if (updatedIssue != null) {
            return ResponseEntity.ok(updatedIssue);
        } else {
            return ResponseEntity.badRequest().body("Issue not found or feedback already submitted");
        }
    }
}
