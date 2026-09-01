package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.CommentDTO;
import com.xueqiu.clone.dto.CommentRequest;
import com.xueqiu.clone.dto.CreatePostRequest;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.service.FeedService;
import com.xueqiu.clone.service.StockService;
import com.xueqiu.clone.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 信息流 / 帖子接口。
 * 写操作（发帖、点赞、评论）需携带 JWT（见 SecurityConfig）。
 */
@RestController
@RequestMapping("/api")
public class FeedController {

    private final FeedService feedService;
    private final StockService stockService;
    private final UserService userService;

    public FeedController(FeedService feedService, StockService stockService, UserService userService) {
        this.feedService = feedService;
        this.stockService = stockService;
        this.userService = userService;
    }

    /** 首页信息流（未登录也能看；登录后会带「当前用户是否点赞」状态）。
     *  type=all 全部；type=following 仅关注的人（需登录，否则返回空）。 */
    @GetMapping("/feed")
    public Page<PostDTO> feed(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "all") String type,
                              Authentication authentication) {
        return feedService.getFeed(page, size, currentUserId(authentication), type);
    }

    /** 发帖（需登录）。作者由当前 JWT 解析 */
    @PostMapping("/posts")
    public PostDTO create(@RequestBody CreatePostRequest req, Authentication authentication) {
        Long uid = userService.findByUsername(authentication.getName()).getId();
        return feedService.addPost(uid, req);
    }

    /** 点赞 / 取消点赞（需登录，按用户持久化） */
    @PostMapping("/feed/{id}/like")
    public PostDTO like(@PathVariable Long id, Authentication authentication) {
        return feedService.toggleLike(id, currentUserId(authentication));
    }

    /** 帖子详情（登录后会带点赞态） */
    @GetMapping("/posts/{id}")
    public PostDTO post(@PathVariable Long id, Authentication authentication) {
        return feedService.getPost(id, currentUserId(authentication));
    }

    /** 帖子评论列表（分页） */
    @GetMapping("/posts/{id}/comments")
    public Page<CommentDTO> comments(@PathVariable Long id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return stockService.getComments(id, page, size);
    }

    /** 发表评论（需登录） */
    @PostMapping("/posts/{id}/comments")
    public CommentDTO addComment(@PathVariable Long id,
                                 @RequestBody CommentRequest req,
                                 Authentication authentication) {
        Long uid = userService.findByUsername(authentication.getName()).getId();
        return stockService.addComment(id, uid, req.content());
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
