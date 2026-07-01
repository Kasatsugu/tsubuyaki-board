package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByPostIdAndClientHash(Long postId, String clientHash);

    boolean existsByPostIdAndClientHash(Long postId, String clientHash);

    long countByPostId(Long postId);
}
