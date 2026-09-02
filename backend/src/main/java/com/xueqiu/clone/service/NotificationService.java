package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.NotificationDTO;
import com.xueqiu.clone.model.Notification;
import com.xueqiu.clone.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知服务：被点赞 / 评论 / 回复 / 关注 / @提及 时生成通知。
 * 自身行为（recipient == actor）不产生通知，避免自提醒。
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** 生成一条通知（跳过给自己发通知的无效场景） */
    @Transactional
    public void notify(Long recipientId, String type, Long actorId, String actorName,
                       String targetType, Long targetId, String text) {
        if (recipientId == null || recipientId.equals(actorId)) return;
        notificationRepository.save(new Notification(recipientId, type, actorId, actorName,
                targetType, targetId, text));
    }

    /** 我的通知（按时间倒序） */
    public List<NotificationDTO> list(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream().map(NotificationDTO::from).toList();
    }

    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    /** 全部标记已读 */
    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllReadByRecipientId(recipientId);
    }
}
