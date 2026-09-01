package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.IndexDTO;
import com.xueqiu.clone.dto.QuoteDTO;
import com.xueqiu.clone.service.QuoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 实时行情接口（后端代理公开行情源，规避浏览器跨域）。 */
@RestController
@RequestMapping("/api")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /** 单只股票实时行情：GET /api/quote/SH600519 */
    @GetMapping("/quote/{symbol}")
    public QuoteDTO quote(@PathVariable String symbol) {
        return quoteService.getQuote(symbol);
    }

    /** 批量行情：GET /api/quotes?symbols=SH600519,SZ300750 */
    @GetMapping("/quotes")
    public Map<String, QuoteDTO> quotes(@RequestParam List<String> symbols) {
        return quoteService.getQuotes(symbols);
    }

    /** 市场指数实时行情：GET /api/indices（数据源不可达时回退内置 Mock） */
    @GetMapping("/indices")
    public List<IndexDTO> indices() {
        return quoteService.getIndexQuotes();
    }
}
