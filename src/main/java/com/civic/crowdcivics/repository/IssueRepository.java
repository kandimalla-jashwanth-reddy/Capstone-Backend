package com.civic.crowdcivics.repository;

import com.civic.crowdcivics.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByReporterEmailOrderByCreatedAtDesc(String reporterEmail);

    List<Issue> findByStatusOrderByCreatedAtDesc(Issue.Status status);

    List<Issue> findByCategoryOrderByCreatedAtDesc(String category);

    List<Issue> findByAssignedDepartmentOrderByCreatedAtDesc(String assignedDepartment);

    long countByStatus(Issue.Status status);

    long countByCategory(String category);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

