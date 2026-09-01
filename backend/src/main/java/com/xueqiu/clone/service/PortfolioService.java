package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.PositionDTO;
import com.xueqiu.clone.dto.QuoteDTO;
import com.xueqiu.clone.model.Position;
import com.xueqiu.clone.model.Watchlist;
import com.xueqiu.clone.repository.PositionRepository;
import com.xueqiu.clone.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 自选股 / 持仓服务。
 * - 自选：按用户维护标的列表，叠加实时行情供行情中心展示。
 * - 持仓：维护份额与成本价，并用当前行情计算市值与浮动盈亏。
 */
@Service
public class PortfolioService {

    private final WatchlistRepository watchlistRepository;
    private final PositionRepository positionRepository;
    private final QuoteService quoteService;

    public PortfolioService(WatchlistRepository watchlistRepository,
                            PositionRepository positionRepository,
                            QuoteService quoteService) {
        this.watchlistRepository = watchlistRepository;
        this.positionRepository = positionRepository;
        this.quoteService = quoteService;
    }

    /** 用户自选标的列表（symbol） */
    public List<String> listWatchlist(Long userId) {
        return watchlistRepository.findByUserIdOrderBySortOrderAscAddedAtDesc(userId)
                .stream().map(Watchlist::getSymbol).toList();
    }

    /** 自选列表 + 实时行情（保持用户自选顺序） */
    public List<QuoteDTO> watchlistQuotes(Long userId) {
        List<String> syms = listWatchlist(userId);
        if (syms.isEmpty()) return List.of();
        Map<String, QuoteDTO> map = quoteService.getQuotes(syms);
        return syms.stream().map(map::get).filter(Objects::nonNull).toList();
    }

    /** 加入 / 取消自选（按用户幂等）。返回最新「是否在自选」 */
    @Transactional
    public boolean toggleWatchlist(Long userId, String symbol) {
        if (watchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
            watchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
            return false;
        }
        int order = watchlistRepository.findByUserIdOrderBySortOrderAscAddedAtDesc(userId).size();
        watchlistRepository.save(new Watchlist(userId, symbol, order));
        return true;
    }

    /** 用户持仓列表（叠加实时行情与盈亏） */
    public List<PositionDTO> listPositions(Long userId) {
        List<Position> positions = positionRepository.findByUserId(userId);
        if (positions.isEmpty()) return List.of();
        List<String> syms = positions.stream().map(Position::getSymbol).toList();
        Map<String, QuoteDTO> map = quoteService.getQuotes(syms);
        return positions.stream()
                .map(p -> PositionDTO.of(p, map.get(p.getSymbol())))
                .toList();
    }

    /** 保存 / 更新持仓（重复 symbol 视为更新份额与成本价） */
    @Transactional
    public PositionDTO upsertPosition(Long userId, String symbol,
                                      java.math.BigDecimal shares, java.math.BigDecimal avgCost) {
        Position pos = positionRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> new Position(userId, symbol, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));
        if (shares != null) pos.setShares(shares);
        if (avgCost != null) pos.setAvgCost(avgCost);
        pos.setUpdatedAt(java.time.LocalDateTime.now());
        pos = positionRepository.save(pos);
        return PositionDTO.of(pos, quoteService.getQuote(symbol));
    }

    /** 移除持仓 */
    @Transactional
    public void removePosition(Long userId, String symbol) {
        positionRepository.deleteByUserIdAndSymbol(userId, symbol);
    }
}
