package com.besthouse.repository;

import com.besthouse.entity.House;
import com.besthouse.entity.enums.HouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseRepository extends JpaRepository<House, Long> {

    List<House> findAllByOrderByCreatedAtDesc();

    List<House> findByStatusOrderByCreatedAtDesc(HouseStatus status);
}
