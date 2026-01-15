package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.RoutingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutingRepository extends JpaRepository<RoutingEntity, Integer> {
}
