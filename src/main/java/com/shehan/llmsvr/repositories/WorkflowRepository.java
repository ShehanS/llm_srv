package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {

    @Query("SELECT w FROM WorkflowEntity w WHERE w.flowId = :id")
    Optional<WorkflowEntity> findByFlowId(@Param("id") String id);

    @Query("SELECT w FROM WorkflowEntity w WHERE w.flowId = :id AND w.flowName = :flowName")
    Optional<WorkflowEntity> findFlow(@Param("id") String id, @Param("flowName") String flowName);
}
