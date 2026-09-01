package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自选股：每位用户可维护一组关注的标的（按证券 symbol 标识）。
 * 同一用户对同一 symbol 唯一（联合唯一约束），删除即取消自选。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_watchlist", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String symbol;

    /** 用户备注（可选，如「长线底仓」） */
    private String note;

    /** 排序权重，越小越靠前 */
    private int sortOrder;

    private LocalDateTime addedAt;

    public Watchlist(Long userId, String symbol, int sortOrder) {
        this.userId = userId;
        this.symbol = symbol;
        this.sortOrder = sortOrder;
        this.addedAt = LocalDateTime.now();
    }
}
