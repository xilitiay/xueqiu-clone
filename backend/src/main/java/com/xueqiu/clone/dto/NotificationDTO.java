package com.xueqiu.clone.dto;

import java.time.LocalDateTime;

/**
 * 通知视图。
 * type：LIKE_POST / COMMENT / REPLY / FOLLOW / MENTION
 * targetType：POST / COMMENT / USER（前端据此拼出跳转链接）
 */
public record NotificationDTO(Long id, String type, Long actorId, String actorName,
                              String targetType, Long targetId, String text,
                              boolean read, LocalDateTime createdAt) {
    public static NotificationDTO from(com.xueqiu.clone.model.Notification n) {
        return new NotificationDTO(n.getId(), n.getType(), n.getActorId(), n.getActorName(),
                n.getTargetType(), n.getTargetId(), n.getText(), n.isRead(), n.getCreatedAt());
    }
}
