package com.xueqiu.clone.dto;

/** 发表评论请求：parentId 为空表示顶层评论，否则为回复（指向所属顶层评论） */
public record CommentRequest(String content, Long parentId) {
}
