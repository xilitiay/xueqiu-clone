package com.xueqiu.clone.dto;

import java.math.BigDecimal;

/** 保存 / 更新持仓请求体 */
public record SavePositionRequest(String symbol, BigDecimal shares, BigDecimal avgCost) {
}
