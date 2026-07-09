package com.assistant.ai.repository;

import com.assistant.ai.model.GeneratedDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedDocRepository extends JpaRepository<GeneratedDoc, Long> {
    List<GeneratedDoc> findByProjectId(Long projectId);
}
