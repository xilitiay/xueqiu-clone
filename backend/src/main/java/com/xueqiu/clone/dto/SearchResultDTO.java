package com.xueqiu.clone.dto;

import java.util.List;

/** 搜索结果聚合：帖子 / 股票 / 用户 */
public record SearchResultDTO(List<PostDTO> posts,
                              List<StockBriefDTO> stocks,
                              List<UserDTO> users) {
}
