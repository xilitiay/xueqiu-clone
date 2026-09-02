package com.xueqiu.clone.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 帖子视图（信息流 / 个股详情 / 收藏列表通用） */
public record PostDTO(Long id, UserDTO author, String content, LocalDateTime createdAt,
                      int likeCount, int commentCount, boolean liked, boolean favorited,
                      List<StockBriefDTO> stocks) {
    public static PostDTO from(com.xueqiu.clone.model.Post p, boolean liked) {
        return from(p, liked, false);
    }

    public static PostDTO from(com.xueqiu.clone.model.Post p, boolean liked, boolean favorited) {
        List<StockBriefDTO> stocks = p.getStocks().stream()
                .map(StockBriefDTO::from)
                .toList();
        return new PostDTO(p.getId(), UserDTO.from(p.getAuthor()), p.getContent(),
                p.getCreatedAt(), p.getLikeCount(), p.getCommentCount(), liked, favorited, stocks);
    }
}
