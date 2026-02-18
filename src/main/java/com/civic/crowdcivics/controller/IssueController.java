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
            Issue updatedIssue = issueService.updateIssueStatus(id, status);
            if (updatedIssue != null) {
                return ResponseEntity.ok(updatedIssue);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status value");
        }
    }
}
