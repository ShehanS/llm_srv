package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.RoutingAgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoutingAgentRepository extends JpaRepository<RoutingAgentEntity, Integer> {
    Optional<RoutingAgentEntity> findByRouteName(String routeName);
}
