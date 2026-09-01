package com.xueqiu.clone.dto;

/** 登录/注册响应：返回 JWT 与基础用户信息 */
public record AuthResponse(String token, String tokenType, Long userId,
                           String username, String name, String avatarColor) {
}
