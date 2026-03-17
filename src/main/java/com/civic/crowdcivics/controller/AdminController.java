package com.civic.crowdcivics.controller;

import com.civic.crowdcivics.model.Issue;
import com.civic.crowdcivics.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AdminController {

        @Autowired
        private IssueService issueService;

        @Autowired
        private com.civic.crowdcivics.service.UserService userService;

        @GetMapping("/summary")
        public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
                List<Issue> allIssues = issueService.getAllIssues();

                Map<String, Object> summary = new HashMap<>();
                summary.put("totalIssues", allIssues.size());
                summary.put("totalCitizens", userService.countUsersByRole("CITIZEN"));
                summary.put("totalStaff", userService.countUsersByRole("ADMIN"));

                Map<String, Long> byStatus = allIssues.stream()
                                .collect(Collectors.groupingBy(issue -> issue.getStatus().name(),
                                                Collectors.counting()));
                summary.put("byStatus", byStatus);

                Map<String, Long> byCategory = allIssues.stream()
                                .collect(Collectors.groupingBy(Issue::getCategory, Collectors.counting()));
                summary.put("byCategory", byCategory);

                double avgResolutionHours = allIssues.stream()
                                .filter(i -> i.getStatus() == Issue.Status.RESOLVED && i.getResolvedAt() != null)
                                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toHours())
                                .average()
                                .orElse(0.0);
                summary.put("avgResolutionHours", avgResolutionHours);

                return ResponseEntity.ok(summary);
        }

        @GetMapping("/users")
        public ResponseEntity<List<com.civic.crowdcivics.model.User>> getAllUsers() {
                return ResponseEntity.ok(userService.findAll());
        }
}
