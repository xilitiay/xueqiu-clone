package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知：有人点赞 / 评论 / 回复 / 关注 / @提及 你时生成。
 * type：LIKE_POST / COMMENT / REPLY / FOLLOW / MENTION
 * targetType：POST / COMMENT / USER（前端据此拼出跳转链接）
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private String type;

    private Long actorId;
    private String actorName;

    private String targetType; // POST / COMMENT / USER
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String text;

    private boolean read = false;

    private LocalDateTime createdAt;

    public Notification(Long recipientId, String type, Long actorId, String actorName,
                         String targetType, Long targetId, String text) {
        this.recipientId = recipientId;
        this.type = type;
        this.actorId = actorId;
        this.actorName = actorName;
        this.targetType = targetType;
        this.targetId = targetId;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
}
