package com.xueqiu.clone.dto;

import java.math.BigDecimal;

/** 个股详情视图（含 K 线序列） */
public record StockDTO(Long id, String symbol, String name, BigDecimal price,
                       BigDecimal changePercent, String market, String industry,
                       double[] kline) {
    public static StockDTO from(com.xueqiu.clone.model.Stock s) {
        double[] arr = new double[0];
        if (s.getKline() != null && !s.getKline().isBlank()) {
            arr = java.util.Arrays.stream(s.getKline().replace("[", "").replace("]", "").split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        }
        return new StockDTO(s.getId(), s.getSymbol(), s.getName(), s.getPrice(),
                s.getChangePercent(), s.getMarket(), s.getIndustry(), arr);
    }
}
