package com.xueqiu.clone.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图（支持嵌套回复与点赞）。
 * parentId 为 null 表示顶层评论；replies 为其回复列表（单层嵌套，回复的回复在展示层归入同一顶层）。
 */
public record CommentDTO(Long id, Long authorId, String authorName, String content, LocalDateTime createdAt,
                          int likeCount, boolean liked, Long parentId, List<CommentDTO> replies) {

    public static CommentDTO from(com.xueqiu.clone.model.Comment c, boolean liked) {
        return from(c, liked, List.of());
    }

    public static CommentDTO from(com.xueqiu.clone.model.Comment c, boolean liked, List<CommentDTO> replies) {
        return new CommentDTO(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(), c.getContent(),
                c.getCreatedAt(), c.getLikeCount(), liked, c.getParentId(),
                replies == null ? List.of() : replies);
    }
}
