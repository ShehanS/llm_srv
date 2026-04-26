package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.AgentToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentToolRepository extends JpaRepository<AgentToolEntity, Integer> {
    boolean existsByToolName(String toolName);

    @Query("SELECT ct FROM AgentToolEntity ct WHERE ct.toolName = :toolName")
    Optional<AgentToolEntity> getToolByName(@Param("toolName") String toolName);
}
