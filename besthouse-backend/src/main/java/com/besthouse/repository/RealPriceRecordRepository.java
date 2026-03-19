package com.besthouse.repository;

import com.besthouse.entity.RealPriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RealPriceRecordRepository extends JpaRepository<RealPriceRecord, Long> {

    @Modifying
    @Query("DELETE FROM RealPriceRecord r WHERE r.cityCode = :cityCode")
    void deleteByCityCode(String cityCode);

    @Query("""
            SELECT r FROM RealPriceRecord r
            WHERE r.transactionDate >= :since
              AND r.totalAreaPing BETWEEN :minPing AND :maxPing
            ORDER BY r.transactionDate DESC
            """)
    List<RealPriceRecord> findCandidates(LocalDate since, BigDecimal minPing, BigDecimal maxPing);

    long countByCityCode(String cityCode);
}
