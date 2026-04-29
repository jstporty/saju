package com.company.saju.saju.adapter.out.persistence.repository;

import com.company.saju.saju.adapter.out.persistence.entity.SajuShareLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SajuShareLinkRepository extends JpaRepository<SajuShareLinkEntity, String> {

    Optional<SajuShareLinkEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE SajuShareLinkEntity s SET s.revokedAt = :now WHERE s.chartId = :chartId AND s.revokedAt IS NULL")
    void revokeAllByChartId(@Param("chartId") String chartId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE SajuShareLinkEntity s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
