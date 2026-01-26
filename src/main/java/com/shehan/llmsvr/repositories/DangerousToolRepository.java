package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.DangerousToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DangerousToolRepository extends JpaRepository<DangerousToolEntity, Integer> {
    Optional<DangerousToolEntity> findByToolName(String toolName);
}
