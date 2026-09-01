package com.xueqiu.clone.dto;

import java.util.List;

/** 用户主页视图：资料 + 该用户发布的帖子。
 *  followers / following 为关注关系实时聚合的真实计数；isFollowing 表示「当前浏览者是否关注了该用户」。 */
public record UserProfileDTO(Long id, String name, String username, String avatarColor, String bio,
                             int followers, int following, int postCount, boolean isFollowing,
                             List<PostDTO> posts) {
    public static UserProfileDTO from(com.xueqiu.clone.model.User u, List<PostDTO> posts,
                                      int followers, int following, boolean isFollowing) {
        return new UserProfileDTO(u.getId(), u.getName(), u.getUsername(), u.getAvatarColor(), u.getBio(),
                followers, following, posts.size(), isFollowing, posts);
    }
}
