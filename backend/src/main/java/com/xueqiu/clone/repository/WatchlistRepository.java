package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    /** 某用户的自选列表（按排序权重 + 添加时间倒序） */
    List<Watchlist> findByUserIdOrderBySortOrderAscAddedAtDesc(Long userId);

    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    void deleteByUserIdAndSymbol(Long userId, String symbol);
}
