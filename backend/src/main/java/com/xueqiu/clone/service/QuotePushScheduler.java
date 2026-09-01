package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.IndexDTO;
import com.xueqiu.clone.dto.QuoteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 定时向订阅的 WebSocket 客户端推送实时行情。
 *
 * - 每 5 秒（上一轮完成后延迟 5s）执行一次，分别推送个股行情到 /topic/quotes、
 *   指数行情到 /topic/indices。
 * - 仅在有客户端连接（SimpUserRegistry 用户数 > 0）时才请求外部行情源，
 *   避免无人订阅时空跑出网、触发行情接口频率限制。
 * - 推送失败（如行情源暂不可用、解析异常）被静默吞掉，单只失败不影响其余，
 *   与 REST 接口的兜底策略保持一致，保证连接稳定。
 */
@Service
public class QuotePushScheduler {

    private final QuoteService quoteService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final List<String> wsSymbols;

    public QuotePushScheduler(QuoteService quoteService,
                              SimpMessagingTemplate messagingTemplate,
                              SimpUserRegistry userRegistry,
                              @Value("${app.quote.ws-symbols:SH600519,SZ300750,SH601318}") String wsSymbols) {
        this.quoteService = quoteService;
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
        this.wsSymbols = Arrays.stream(wsSymbols.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Scheduled(fixedDelay = 5000)
    public void pushQuotes() {
        // 无订阅者则不请求外部行情，避免无效出网与频率限制
        if (userRegistry.getUserCount() == 0) return;

        try {
            Map<String, QuoteDTO> quotes = quoteService.getQuotes(wsSymbols);
            if (!quotes.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/quotes", quotes);
            }
        } catch (Exception ignored) {
            // 个股行情失败不阻断指数推送
        }

        try {
            List<IndexDTO> indices = quoteService.getIndexQuotes();
            messagingTemplate.convertAndSend("/topic/indices", indices);
        } catch (Exception ignored) {
            // 指数行情失败静默处理
        }
    }
}
