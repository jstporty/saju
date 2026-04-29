package com.company.saju.saju.adapter.out.persistence.repository;

import com.company.saju.saju.adapter.out.persistence.entity.KeyMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KeyMessageRepository extends JpaRepository<KeyMessageEntity, Long> {

    Optional<KeyMessageEntity> findByDayStemAndDominantElementAndCategory(
            String dayStem, String dominantElement, String category);

    @Query("SELECT k FROM KeyMessageEntity k WHERE k.dayStem = :dayStem AND k.dominantElement = :dominantElement")
    List<KeyMessageEntity> findAll6Categories(
            @Param("dayStem") String dayStem,
            @Param("dominantElement") String dominantElement);
}
