package com.xueqiu.clone.dto;

import java.math.BigDecimal;

/**
 * 实时行情视图。
 * 数据来自后端代理的公开行情接口（腾讯 gtimg，无需授权 key），
 * 仅在交易时段实时更新；非交易时段返回最近收盘数据。
 */
public record QuoteDTO(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal prevClose,
        Long volume,            // 成交量（手）
        BigDecimal turnoverRate, // 换手率（%）
        BigDecimal pe,          // 市盈率(TTM)
        BigDecimal pb,          // 市净率
        BigDecimal marketCap,   // 总市值（亿元）
        String time             // 行情时间
) {
}
