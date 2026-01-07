package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginRepository extends JpaRepository<ToolEntity, Long> {

}
