package com.company.saju.auth.adapter.out.persistence.repository;

import com.company.saju.auth.adapter.out.persistence.entity.OAuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthUserRepository extends JpaRepository<OAuthUserEntity, String> {

    Optional<OAuthUserEntity> findByEmail(String email);

    Optional<OAuthUserEntity> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);
}
