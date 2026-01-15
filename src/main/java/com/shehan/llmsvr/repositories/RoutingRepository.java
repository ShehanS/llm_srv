package com.shehan.llmsvr.repositories;

import com.shehan.llmsvr.entites.RoutingConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoutingRepository extends JpaRepository<RoutingConfigEntity, Integer> {
    Optional<RoutingConfigEntity> findByRouteName(String routeName);
}
