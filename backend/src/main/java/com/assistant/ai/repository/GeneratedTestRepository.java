package com.assistant.ai.repository;

import com.assistant.ai.model.GeneratedTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedTestRepository extends JpaRepository<GeneratedTest, Long> {
    List<GeneratedTest> findByProjectId(Long projectId);
}
