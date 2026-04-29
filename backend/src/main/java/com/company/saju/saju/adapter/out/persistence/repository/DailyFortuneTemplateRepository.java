package com.company.saju.saju.adapter.out.persistence.repository;

import com.company.saju.saju.adapter.out.persistence.entity.DailyFortuneTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyFortuneTemplateRepository extends JpaRepository<DailyFortuneTemplateEntity, Long> {

    Optional<DailyFortuneTemplateEntity> findByDayStemAndDailyStemAndDailyBranch(
            String dayStem, String dailyStem, String dailyBranch);
}
