package com.civic.crowdcivics.service;

import com.civic.crowdcivics.exception.VerificationException;
import com.civic.crowdcivics.model.Issue;
import com.civic.crowdcivics.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageAnalysisService imageAnalysisService;

    public Issue createIssue(Issue issue) {
        // Enforce Image Verification
        if (issue.getPhotoUrl() != null && issue.getPhotoUrl().startsWith("data:image/")) {
            try {
                Map<String, Object> analysis = imageAnalysisService.analyzeBase64Image(issue.getPhotoUrl());
                String identifiedCategory = (String) analysis.getOrDefault("identified_category", "OTHER");

                if ("OTHER".equalsIgnoreCase(identifiedCategory) || !((boolean) analysis.getOrDefault("isValid", true))) {
                    String reason = (String) analysis.getOrDefault("rejectionReason", 
                            "The uploaded photo does not appear to be a valid civic issue.");
                    throw new VerificationException("Verification Failed: " + reason);
                }

                // Auto-fix category if user selected OTHER but AI identified a specific one
                if ("OTHER".equalsIgnoreCase(issue.getCategory()) && !"OTHER".equalsIgnoreCase(identifiedCategory)) {
                    issue.setCategory(identifiedCategory);
                }

            } catch (IOException e) {
                System.err.println("Verification failed due to error: " + e.getMessage());
                throw new VerificationException("Image verification service is currently unavailable. Please try again later.");
            }
        }

        // Populate reporter name if missing
        if ((issue.getReporterName() == null || issue.getReporterName().trim().isEmpty())
                && issue.getReporterId() != null) {
            userService.findById(issue.getReporterId()).ifPresent(user -> {
                issue.setReporterName(user.getName());
            });
        } else if ((issue.getReporterName() == null || issue.getReporterName().trim().isEmpty())
                && issue.getReporterEmail() != null) {
            userService.findByEmail(issue.getReporterEmail()).ifPresent(user -> {
                issue.setReporterName(user.getName());
            });
        }

        issue.setAssignedDepartment(determineDepartment(issue.getCategory()));
        return issueRepository.save(issue);
    }

    public List<Issue> getAllIssues() {
        List<Issue> issues = issueRepository.findAll();
        populateReporterNames(issues);
        return issues;
    }

    public List<Issue> getIssuesByReporter(Long reporterId) {
        List<Issue> issues = issueRepository.findByReporterId(reporterId);
        populateReporterNames(issues);
        return issues;
    }

    public Optional<Issue> getIssueById(Long id) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        issueOpt.ifPresent(this::populateReporterName);
        return issueOpt;
    }

    public List<Issue> getIssuesByDepartment(String department) {
        List<Issue> issues = issueRepository.findByAssignedDepartment(department);
        populateReporterNames(issues);
        return issues;
    }

    private void populateReporterNames(List<Issue> issues) {
        for (Issue issue : issues) {
            populateReporterName(issue);
        }
    }

    private void populateReporterName(Issue issue) {
        if (issue.getReporterName() == null || issue.getReporterName().trim().isEmpty() ||
                "Anonymous Citizen".equalsIgnoreCase(issue.getReporterName().trim())) {

            if (issue.getReporterId() != null) {
                userService.findById(issue.getReporterId()).ifPresent(user -> {
                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        System.out.println("Enriching issue " + issue.getId() + " with name: " + user.getName());
                        issue.setReporterName(user.getName());
                    }
                });
            } else if (issue.getReporterEmail() != null) {
                userService.findByEmail(issue.getReporterEmail()).ifPresent(user -> {
                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        System.out.println(
                                "Enriching issue " + issue.getId() + " with name (via email): " + user.getName());
                        issue.setReporterName(user.getName());
                    }
                });
            }
        }
    }

    public Issue updateIssueStatus(Long id, Issue.Status status, String resolutionPhotoUrl, String rejectionReason) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        if (issueOpt.isPresent()) {
            Issue issue = issueOpt.get();
            issue.setStatus(status);
            if (status == Issue.Status.RESOLVED && resolutionPhotoUrl != null && !resolutionPhotoUrl.isEmpty()) {
                issue.setResolutionPhotoUrl(resolutionPhotoUrl);
            }
            if (status == Issue.Status.REJECTED && rejectionReason != null && !rejectionReason.isEmpty()) {
                issue.setRejectionReason(rejectionReason);
            }
            return issueRepository.save(issue);
        }
        return null;
    }

    public Issue submitFeedback(Long id, String feedback, Integer rating) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        if (issueOpt.isPresent()) {
            Issue issue = issueOpt.get();
            if (issue.getRating() == null) {
                issue.setFeedback(feedback);
                issue.setRating(rating);
                return issueRepository.save(issue);
            }
        }
        return null;
    }

    private String determineDepartment(String category) {
        if (category == null)
            return "General Administration";
        switch (category.toUpperCase()) {
            case "POTHOLE":
            case "ROADS":
                return "Roads Department";
            case "STREETLIGHT":
            case "ELECTRICITY":
                return "Electricity Board";
            case "TRASH":
            case "GARBAGE":
            case "SANITATION":
                return "Sanitation";
            case "WATER":
            case "PIPE":
                return "Water Department";
            default:
                return "Municipality (General)";
        }
    }
}
