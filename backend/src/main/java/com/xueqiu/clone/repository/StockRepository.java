package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);

    /** 搜索：名称或代码包含关键字（忽略大小写） */
    List<Stock> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(String name, String symbol);
}
