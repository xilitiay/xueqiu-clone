package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.CreatePostRequest;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.PostLike;
import com.xueqiu.clone.model.Stock;
import com.xueqiu.clone.model.User;
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
import java.util.List;

/**
 * 信息流服务。
 * 点赞状态按用户持久化（PostLike 表），likeCount 作为非规范化总计数维护；
 * 未登录（userId 为 null）时一律视为未点赞。
 * 信息流支持 type=all（全部）与 type=following（仅关注的人，需登录）。
 */
@Service
public class FeedService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;

    public FeedService(PostRepository postRepository, UserRepository userRepository,
                       StockRepository stockRepository, PostLikeRepository postLikeRepository,
                       UserService userService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.postLikeRepository = postLikeRepository;
        this.userService = userService;
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
                    .map(p -> PostDTO.from(p, liked(p.getId(), userId)));
        }
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> PostDTO.from(p, liked(p.getId(), userId)));
    }

    public PostDTO getPost(Long id, Long userId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + id));
        return PostDTO.from(p, liked(id, userId));
    }

    /** 点赞 / 取消点赞（按用户幂等）。返回最新点赞数与当前用户点赞态 */
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
        return PostDTO.from(p, nowLiked);
    }

    /** 发帖：关联已知股票代码（未知代码忽略），作者由调用方解析后的 userId 指定 */
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
        return PostDTO.from(p, false);
    }

    private boolean liked(Long postId, Long userId) {
        return userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
