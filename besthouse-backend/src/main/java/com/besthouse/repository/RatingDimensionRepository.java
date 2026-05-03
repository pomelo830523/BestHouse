package com.besthouse.repository;

import com.besthouse.entity.RatingDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingDimensionRepository extends JpaRepository<RatingDimension, Long> {

    List<RatingDimension> findByIsActiveTrueOrderByWeightDescSortOrderAsc();
}
