package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.CreatePostRequest;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.model.Favorite;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.PostLike;
import com.xueqiu.clone.model.Stock;
import com.xueqiu.clone.model.User;
import com.xueqiu.clone.repository.FavoriteRepository;
import com.xueqiu.clone.repository.PostLikeRepository;
import com.xueqiu.clone.repository.PostRepository;
import com.xueqiu.clone.repository.StockRepository;
import com.xueqiu.clone.repository.UserRepository;
import com.xueqiu.clone.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 信息流服务。
 * - 点赞状态按用户持久化（PostLike 表），likeCount 作为非规范化总计数维护；
 * - 收藏状态按用户持久化（Favorite 表）；
 * - 点赞 / 发帖 @提及 会生成通知。
 * 未登录（userId 为 null）时一律视为未点赞 / 未收藏。
 * 信息流支持 type=all（全部）与 type=following（仅关注的人，需登录）。
 */
@Service
public class FeedService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PostLikeRepository postLikeRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public FeedService(PostRepository postRepository, UserRepository userRepository,
                       StockRepository stockRepository, PostLikeRepository postLikeRepository,
                       FavoriteRepository favoriteRepository, UserService userService,
                       NotificationService notificationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.postLikeRepository = postLikeRepository;
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    public Page<PostDTO> getFeed(int page, int size, Long userId) {
        return getFeed(page, size, userId, "all");
    }

    public Page<PostDTO> getFeed(int page, int size, Long userId, String type) {
        Pageable pageable = PageRequest.of(page, size);
        if ("following".equalsIgnoreCase(type) && userId != null) {
            List<Long> ids = userService.followingIds(userId);
            if (ids.isEmpty()) return Page.empty(pageable);
            return postRepository.findByAuthorIdInOrderByCreatedAtDesc(ids, pageable)
                    .map(p -> toDTO(p, userId));
        }
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> toDTO(p, userId));
    }

    public PostDTO getPost(Long id, Long userId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + id));
        return toDTO(p, userId);
    }

    /** 点赞 / 取消点赞（按用户幂等）。返回最新点赞数与当前用户点赞态；被点赞者收到通知 */
    @Transactional
    public PostDTO toggleLike(Long id, Long userId) {
        if (userId == null) throw new IllegalStateException("请先登录后再点赞");
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + id));
        boolean nowLiked;
        if (postLikeRepository.existsByPostIdAndUserId(id, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(id, userId);
            p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
            nowLiked = false;
        } else {
            postLikeRepository.save(new PostLike(id, userId));
            p.setLikeCount(p.getLikeCount() + 1);
            nowLiked = true;
        }
        postRepository.save(p);

        if (nowLiked) {
            userRepository.findById(userId).ifPresent(me ->
                    notificationService.notify(p.getAuthor().getId(), "LIKE_POST", userId, me.getName(),
                            "POST", id, "赞了你的帖子"));
        }
        return toDTO(p, userId);
    }

    /** 收藏 / 取消收藏（按用户幂等）。返回最新收藏态 */
    @Transactional
    public PostDTO toggleFavorite(Long id, Long userId) {
        if (userId == null) throw new IllegalStateException("请先登录后再收藏");
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + id));
        if (favoriteRepository.existsByPostIdAndUserId(id, userId)) {
            favoriteRepository.deleteByPostIdAndUserId(id, userId);
        } else {
            favoriteRepository.save(new Favorite(id, userId));
        }
        return toDTO(p, userId);
    }

    /** 发帖：关联已知股票代码（未知代码忽略），正文 @提及 的用户会收到通知 */
    @Transactional
    public PostDTO addPost(Long authorId, CreatePostRequest req) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + authorId));
        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty()) throw new IllegalArgumentException("正文不能为空");

        List<Stock> stocks = new ArrayList<>();
        if (req.symbols() != null) {
            for (String sym : req.symbols()) {
                if (sym == null || sym.isBlank()) continue;
                stockRepository.findBySymbol(sym.trim().toUpperCase()).ifPresent(stocks::add);
            }
        }
        Post p = new Post(author, content, LocalDateTime.now(), 0, 0, stocks);
        p = postRepository.save(p);

        notifyMentions(content, authorId, author.getName(), "POST", p.getId());
        return PostDTO.from(p, false, false);
    }

    /** 解析正文中的 @用户名 并生成 MENTION 通知（跳过自己与不存在的用户） */
    private void notifyMentions(String text, Long actorId, String actorName,
                                String targetType, Long targetId) {
        Matcher m = Pattern.compile("@([A-Za-z0-9_]{2,32})").matcher(text);
        Set<String> names = new LinkedHashSet<>();
        while (m.find()) names.add(m.group(1));
        for (String uname : names) {
            userRepository.findByUsername(uname).ifPresent(u ->
                    notificationService.notify(u.getId(), "MENTION", actorId, actorName,
                            targetType, targetId, "在内容中提到了你"));
        }
    }

    private PostDTO toDTO(Post p, Long userId) {
        return PostDTO.from(p, liked(p.getId(), userId), favorited(p.getId(), userId));
    }

    private boolean liked(Long postId, Long userId) {
        return userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    private boolean favorited(Long postId, Long userId) {
        return userId != null && favoriteRepository.existsByPostIdAndUserId(postId, userId);
    }
}
