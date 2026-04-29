package com.company.saju.user.adapter.out.persistence.repository;

import com.company.saju.user.adapter.out.persistence.entity.UserJpaEntity;
import com.company.saju.user.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    
    Optional<UserJpaEntity> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<UserJpaEntity> findByStatus(UserStatus status);
}
