package com.xueqiu.clone.dto;

import java.math.BigDecimal;

/** 股票精简视图（用于信息流中的标签） */
public record StockBriefDTO(String symbol, String name, BigDecimal changePercent) {
    public static StockBriefDTO from(com.xueqiu.clone.model.Stock s) {
        return new StockBriefDTO(s.getSymbol(), s.getName(), s.getChangePercent());
    }
}
