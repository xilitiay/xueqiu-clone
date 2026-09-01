package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.PositionDTO;
import com.xueqiu.clone.dto.QuoteDTO;
import com.xueqiu.clone.dto.SavePositionRequest;
import com.xueqiu.clone.service.PortfolioService;
import com.xueqiu.clone.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自选股 / 持仓接口（均需登录）。
 * 行情取不到时后端回落示例数据，前端侧栏与行情中心始终有内容。
 */
@RestController
@RequestMapping("/api")
public class WatchlistController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    public WatchlistController(PortfolioService portfolioService, UserService userService) {
        this.portfolioService = portfolioService;
        this.userService = userService;
    }

    /** 我的自选（含实时行情） */
    @GetMapping("/watchlist")
    public List<QuoteDTO> watchlist(Authentication authentication) {
        return portfolioService.watchlistQuotes(requireLogin(authentication));
    }

    /** 加入自选 */
    @PostMapping("/watchlist/{symbol}")
    public Map<String, Object> addWatch(@PathVariable String symbol, Authentication authentication) {
        boolean added = portfolioService.toggleWatchlist(requireLogin(authentication), symbol.toUpperCase());
        return Map.of("added", added);
    }

    /** 取消自选 */
    @DeleteMapping("/watchlist/{symbol}")
    public Map<String, Object> removeWatch(@PathVariable String symbol, Authentication authentication) {
        portfolioService.toggleWatchlist(requireLogin(authentication), symbol.toUpperCase());
        return Map.of("removed", true);
    }

    /** 我的持仓（含实时盈亏） */
    @GetMapping("/positions")
    public List<PositionDTO> positions(Authentication authentication) {
        return portfolioService.listPositions(requireLogin(authentication));
    }

    /** 保存 / 更新持仓 */
    @PostMapping("/positions")
    public PositionDTO savePosition(@RequestBody SavePositionRequest req, Authentication authentication) {
        if (req.symbol() == null || req.symbol().isBlank()) throw new IllegalArgumentException("symbol 不能为空");
        return portfolioService.upsertPosition(requireLogin(authentication),
                req.symbol().toUpperCase(), req.shares(), req.avgCost());
    }

    /** 移除持仓 */
    @DeleteMapping("/positions/{symbol}")
    public Map<String, Object> removePosition(@PathVariable String symbol, Authentication authentication) {
        portfolioService.removePosition(requireLogin(authentication), symbol.toUpperCase());
        return Map.of("removed", true);
    }

    private Long requireLogin(Authentication authentication) {
        Long uid = currentUserId(authentication);
        if (uid == null) throw new IllegalStateException("请先登录后再操作自选 / 持仓");
        return uid;
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return userService.findByUsername(authentication.getName()).getId();
        } catch (Exception e) {
            return null;
        }
    }
}
