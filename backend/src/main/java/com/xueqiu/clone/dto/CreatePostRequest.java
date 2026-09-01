package com.xueqiu.clone.dto;

import java.util.List;

/** 发帖请求：正文 + 关联的股票代码（可选） */
public record CreatePostRequest(String content, List<String> symbols) {
}
