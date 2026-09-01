package com.xueqiu.clone.dto;

import java.util.List;

/** 用户主页视图：资料 + 该用户发布的帖子 */
public record UserProfileDTO(Long id, String name, String avatarColor, String bio,
                             int followers, int postCount, List<PostDTO> posts) {
    public static UserProfileDTO from(com.xueqiu.clone.model.User u, List<PostDTO> posts) {
        return new UserProfileDTO(u.getId(), u.getName(), u.getAvatarColor(), u.getBio(),
                u.getFollowers(), posts.size(), posts);
    }
}
