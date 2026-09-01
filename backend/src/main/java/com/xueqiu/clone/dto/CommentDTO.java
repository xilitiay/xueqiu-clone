package com.xueqiu.clone.dto;

import java.time.LocalDateTime;

/** 评论视图 */
public record CommentDTO(Long id, String authorName, String content, LocalDateTime createdAt) {
    public static CommentDTO from(com.xueqiu.clone.model.Comment c) {
        return new CommentDTO(c.getId(), c.getAuthor().getName(), c.getContent(), c.getCreatedAt());
    }
}
