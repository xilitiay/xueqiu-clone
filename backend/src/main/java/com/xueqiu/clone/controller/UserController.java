package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.UserDTO;
import com.xueqiu.clone.dto.UserProfileDTO;
import com.xueqiu.clone.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 用户主页 / 关注关系接口。 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户主页：支持 /api/users/{id} 或 /api/users/{username}；登录后会带「是否已关注」状态 */
    @GetMapping("/users/{id}")
    public UserProfileDTO user(@PathVariable String id, Authentication authentication) {
        Long uid = userService.resolveUserId(id);
        return userService.getProfile(uid, currentUserId(authentication));
    }

    /** 关注 / 取消关注（需登录，按用户幂等）。返回最新「是否关注」 */
    @PostMapping("/users/{id}/follow")
    public Map<String, Object> follow(@PathVariable String id, Authentication authentication) {
        Long me = currentUserId(authentication);
        if (me == null) throw new IllegalStateException("请先登录后再关注");
        boolean following = userService.toggleFollow(me, userService.resolveUserId(id));
        return Map.of("following", following);
    }

    /** 该用户关注的人 */
    @GetMapping("/users/{id}/following")
    public List<UserDTO> following(@PathVariable String id) {
        return userService.listFollowing(userService.resolveUserId(id));
    }

    /** 该用户的粉丝 */
    @GetMapping("/users/{id}/followers")
    public List<UserDTO> followers(@PathVariable String id) {
        return userService.listFollowers(userService.resolveUserId(id));
    }

    /** 我的收藏（需登录，按收藏时间倒序） */
    @GetMapping("/favorites")
    public List<PostDTO> favorites(Authentication authentication) {
        Long me = currentUserId(authentication);
        if (me == null) throw new IllegalStateException("请先登录后再查看收藏");
        return userService.listFavorites(me);
    }

    /** 从 Spring Security 的 Authentication 解析当前用户 id；匿名 / 未登录返回 null */
    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return userService.findByUsername(authentication.getName()).getId();
        } catch (Exception e) {
            return null;
        }
    }
}
