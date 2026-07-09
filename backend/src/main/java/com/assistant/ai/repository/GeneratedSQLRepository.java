package com.assistant.ai.repository;

import com.assistant.ai.model.GeneratedSQL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedSQLRepository extends JpaRepository<GeneratedSQL, Long> {
    List<GeneratedSQL> findByProjectId(Long projectId);
}
