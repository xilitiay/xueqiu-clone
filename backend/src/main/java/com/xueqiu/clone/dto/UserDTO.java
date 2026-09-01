package com.xueqiu.clone.dto;

/** 用户精简视图 */
public record UserDTO(Long id, String name, String avatarColor, String bio, int followers) {
    public static UserDTO from(com.xueqiu.clone.model.User u) {
        return new UserDTO(u.getId(), u.getName(), u.getAvatarColor(), u.getBio(), u.getFollowers());
    }
}
