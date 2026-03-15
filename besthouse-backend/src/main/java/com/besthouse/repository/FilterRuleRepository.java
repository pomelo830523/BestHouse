package com.besthouse.repository;

import com.besthouse.entity.FilterRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FilterRuleRepository extends JpaRepository<FilterRule, Long> {

    List<FilterRule> findByIsActiveTrueOrderByRuleId();
}
