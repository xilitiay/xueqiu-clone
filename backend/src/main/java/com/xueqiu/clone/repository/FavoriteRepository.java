package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
