package com.xueqiu.clone.dto;

import com.xueqiu.clone.model.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 持仓视图：在持仓记录基础上叠加实时行情，计算市值、成本、浮动盈亏与盈亏比例。
 * 行情取不到时，以成本价兜底（盈亏计为 0），保证页面始终有数据。
 */
public record PositionDTO(Long id, String symbol, String name, BigDecimal shares, BigDecimal avgCost,
                          BigDecimal price, BigDecimal changePercent, BigDecimal marketValue,
                          BigDecimal costValue, BigDecimal pnl, BigDecimal pnlPercent, String market) {

    public static PositionDTO of(Position p, QuoteDTO q) {
        BigDecimal price = (q != null && q.price() != null) ? q.price() : p.getAvgCost();
        BigDecimal shares = p.getShares();
        BigDecimal cost = p.getAvgCost();

        BigDecimal costValue = (cost != null && shares != null) ? cost.multiply(shares) : null;
        BigDecimal marketValue = (price != null && shares != null) ? price.multiply(shares) : null;
        BigDecimal pnl = (marketValue != null && costValue != null) ? marketValue.subtract(costValue) : null;
        BigDecimal pnlPercent = (pnl != null && costValue != null && costValue.signum() != 0)
                ? pnl.multiply(new BigDecimal("100")).divide(costValue, 2, RoundingMode.HALF_UP)
                : null;

        String name = (q != null && q.name() != null) ? q.name() : p.getSymbol();
        return new PositionDTO(p.getId(), p.getSymbol(), name, shares, cost, price,
                (q != null ? q.changePercent() : null), marketValue, costValue, pnl, pnlPercent,
                (q != null ? null : null));
    }
}
