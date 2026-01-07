package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<AgentEntity, Long> {
    Optional<AgentEntity> findByAgentName(String agentName);
}
