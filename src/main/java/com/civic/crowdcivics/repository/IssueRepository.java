package com.civic.crowdcivics.repository;

import com.civic.crowdcivics.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByReporterId(Long reporterId);

    List<Issue> findByStatus(Issue.Status status);

    List<Issue> findByCategory(String category);
}
