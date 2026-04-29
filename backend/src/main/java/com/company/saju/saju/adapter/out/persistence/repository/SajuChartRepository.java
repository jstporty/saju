package com.company.saju.saju.adapter.out.persistence.repository;

import com.company.saju.saju.adapter.out.persistence.entity.SajuChartEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// @SQLRestriction("deleted_at IS NULL") on the entity auto-filters all queries below.
public interface SajuChartRepository extends JpaRepository<SajuChartEntity, String> {

    /** 차트 단건 조회 (@SQLRestriction이 deleted_at IS NULL 자동 적용). */
    Optional<SajuChartEntity> findById(String id);

    /** 사용자 소유 차트 목록 (offset 페이지네이션). */
    @Query("SELECT c FROM SajuChartEntity c WHERE c.userId = :userId " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<SajuChartEntity> findByUserId(@Param("userId") String userId, Pageable pageable);

    /** calculation_key 중복 체크. */
    Optional<SajuChartEntity> findByUserIdAndCalculationKey(String userId, String calculationKey);

    /** 사용자의 가장 최근 차트 ID (로그인 후 redirect용). */
    @Query("SELECT c.id FROM SajuChartEntity c WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    List<String> findLatestChartIdByUserId(@Param("userId") String userId, Pageable pageable);

    /** 사용자 탈퇴 시 해당 유저의 미삭제 차트 전체 조회. */
    List<SajuChartEntity> findAllByUserId(String userId);
}
