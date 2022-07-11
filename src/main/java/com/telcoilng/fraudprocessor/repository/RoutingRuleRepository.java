package com.telcoilng.fraudprocessor.repository;

import com.telcoilng.fraudprocessor.Entity.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule,Long> {
    List<RoutingRule> findByUserIdAndDeleted(String userId, boolean deleted);
}
