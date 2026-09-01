package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByUserId(Long userId);

    Optional<Position> findByUserIdAndSymbol(Long userId, String symbol);

    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    void deleteByUserIdAndSymbol(Long userId, String symbol);
}
