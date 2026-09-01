package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.StockDTO;
import com.xueqiu.clone.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 个股接口：热门榜、详情、相关讨论。
 */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /** 热门股票榜 */
    @GetMapping("/hot")
    public java.util.List<StockDTO> hot(@RequestParam(defaultValue = "8") int limit) {
        return stockService.getHotStocks(limit);
    }

    /** 个股详情（含 K 线） */
    @GetMapping("/{symbol}")
    public StockDTO detail(@PathVariable String symbol) {
        return stockService.getStock(symbol);
    }

    /** 个股相关帖子 */
    @GetMapping("/{symbol}/posts")
    public Page<PostDTO> posts(@PathVariable String symbol,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return stockService.getStockPosts(symbol, page, size);
    }
}
