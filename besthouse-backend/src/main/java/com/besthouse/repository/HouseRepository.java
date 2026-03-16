package com.besthouse.repository;

import com.besthouse.entity.House;
import com.besthouse.entity.enums.HouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {

    List<House> findAllByOrderByCreatedAtDesc();

    List<House> findByStatusOrderByCreatedAtDesc(HouseStatus status);

    Optional<House> findByListingUrl(String listingUrl);
}
