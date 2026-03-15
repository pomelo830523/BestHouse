package com.besthouse.repository;

import com.besthouse.entity.HouseRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HouseRatingRepository extends JpaRepository<HouseRating, Long> {

    @Query("SELECT r FROM HouseRating r JOIN FETCH r.member JOIN FETCH r.dimension WHERE r.house.houseId = :houseId")
    List<HouseRating> findByHouseIdWithDetails(@Param("houseId") Long houseId);

    Optional<HouseRating> findByHouseHouseIdAndMemberMemberIdAndDimensionDimensionId(
            Long houseId, Long memberId, Long dimensionId);

    @Query("SELECT r FROM HouseRating r JOIN FETCH r.house JOIN FETCH r.member JOIN FETCH r.dimension")
    List<HouseRating> findAllWithDetails();

    void deleteByHouseHouseId(Long houseId);
}
