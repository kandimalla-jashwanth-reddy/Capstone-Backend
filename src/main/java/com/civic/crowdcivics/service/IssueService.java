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

    public Issue updateIssueStatus(Long id, Issue.Status status) {
        Optional<Issue> issueOpt = issueRepository.findById(id);
        if (issueOpt.isPresent()) {
            Issue issue = issueOpt.get();
            issue.setStatus(status);
            return issueRepository.save(issue);
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
