package com.civic.crowdcivics.service;

import com.civic.crowdcivics.model.Issue;
import com.civic.crowdcivics.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    public Issue createIssue(Issue issue) {
        issue.setAssignedDepartment(determineDepartment(issue.getCategory()));
        return issueRepository.save(issue);
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public List<Issue> getIssuesByReporter(Long reporterId) {
        return issueRepository.findByReporterId(reporterId);
    }

    public Optional<Issue> getIssueById(Long id) {
        return issueRepository.findById(id);
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
                return "Sanitation Department";
            case "WATER":
            case "PIPE":
                return "Water Works";
            default:
                return "General Administration";
        }
    }
}
