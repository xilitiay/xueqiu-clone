package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓：用户持有的某只标的（份额 + 成本价），用于组合盈亏展示。
 * 同一用户对同一 symbol 唯一，重复保存视为更新。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_position", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String symbol;

    /** 持有份额（股 / 份） */
    private BigDecimal shares;

    /** 持仓成本价 */
    private BigDecimal avgCost;

    private LocalDateTime updatedAt;

    public Position(Long userId, String symbol, BigDecimal shares, BigDecimal avgCost) {
        this.userId = userId;
        this.symbol = symbol;
        this.shares = shares;
        this.avgCost = avgCost;
        this.updatedAt = LocalDateTime.now();
    }
}
