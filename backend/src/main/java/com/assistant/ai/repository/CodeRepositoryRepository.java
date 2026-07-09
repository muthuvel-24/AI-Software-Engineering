package com.assistant.ai.repository;

import com.assistant.ai.model.CodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {
    List<CodeRepository> findByProjectId(Long projectId);
}
