package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 股票（个股）。
 * kline 以 JSON 数组字符串存储（收盘价序列），前端绘制迷你走势图。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 证券代码，如 SH600519 */
    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;

    /** 涨跌幅，如 2.35 表示 +2.35% */
    private BigDecimal changePercent;

    private String market;   // 沪市 / 深市 / 美股 ...
    private String industry; // 行业

    @Column(columnDefinition = "TEXT")
    private String kline;    // JSON 数组，如 [1800,1820,...]

    public Stock(String symbol, String name, BigDecimal price, BigDecimal changePercent,
                 String market, String industry, String kline) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.changePercent = changePercent;
        this.market = market;
        this.industry = industry;
        this.kline = kline;
    }
}
