package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.NotificationDTO;
import com.xueqiu.clone.service.NotificationService;
import com.xueqiu.clone.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知接口：被点赞 / 评论 / 回复 / 关注 / @提及 时生成的消息。
 * 通知由各业务服务（FeedService / StockService / UserService）在相应行为发生时写入。
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /** 我的通知（按时间倒序） */
    @GetMapping("/notifications")
    public List<NotificationDTO> list(Authentication authentication) {
        return notificationService.list(requireLogin(authentication));
    }

    /** 未读数（用于头部红点角标） */
    @GetMapping("/notifications/unread")
    public Map<String, Long> unread(Authentication authentication) {
        return Map.of("count", notificationService.unreadCount(requireLogin(authentication)));
    }

    /** 全部标记已读 */
    @PostMapping("/notifications/read")
    public Map<String, Object> readAll(Authentication authentication) {
        notificationService.markAllRead(requireLogin(authentication));
        return Map.of("ok", true);
    }

    private Long requireLogin(Authentication authentication) {
        Long uid = currentUserId(authentication);
        if (uid == null) throw new IllegalStateException("请先登录后再查看通知");
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
