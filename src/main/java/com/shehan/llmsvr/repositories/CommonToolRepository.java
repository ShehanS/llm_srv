package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.CommonToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CommonToolRepository extends JpaRepository<CommonToolEntity, Long> {

    @Query("SELECT ct FROM CommonToolEntity ct WHERE ct.toolName = :toolName")
    CommonToolEntity getToolByName(@Param("toolName") String toolName);
}
