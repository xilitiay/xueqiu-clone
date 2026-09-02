package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.CommentDTO;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.StockDTO;
import com.xueqiu.clone.model.Comment;
import com.xueqiu.clone.model.CommentLike;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.Stock;
import com.xueqiu.clone.model.User;
import com.xueqiu.clone.repository.CommentLikeRepository;
import com.xueqiu.clone.repository.CommentRepository;
import com.xueqiu.clone.repository.PostRepository;
import com.xueqiu.clone.repository.StockRepository;
import com.xueqiu.clone.repository.UserRepository;
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
 * 个股服务：热门列表、详情、相关帖子、评论（嵌套回复 + 点赞）。
 * 评论相关行为（评论/回复/@提及）会生成通知。
 */
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;

    public StockService(StockRepository stockRepository, PostRepository postRepository,
                        CommentRepository commentRepository, UserRepository userRepository,
                        CommentLikeRepository commentLikeRepository,
                        NotificationService notificationService) {
        this.stockRepository = stockRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.notificationService = notificationService;
    }

    /** 热门股票：按涨跌幅绝对值排序取前 N */
    public List<StockDTO> getHotStocks(int limit) {
        return stockRepository.findAll().stream()
                .sorted((a, b) -> b.getChangePercent().abs().compareTo(a.getChangePercent().abs()))
                .limit(limit)
                .map(StockDTO::from)
                .toList();
    }

    public StockDTO getStock(String symbol) {
        Stock s = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("股票不存在: " + symbol));
        return StockDTO.from(s);
    }

    public Page<PostDTO> getStockPosts(String symbol, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByStockSymbol(symbol, pageable)
                .map(p -> PostDTO.from(p, false));
    }

    /**
     * 帖子评论树：顶层评论（parentId 为空）+ 其回复，回复内嵌在 replies 中（单层嵌套）。
     * 登录后会带「当前用户对每条评论是否已点赞」状态。
     */
    public List<CommentDTO> getCommentTree(Long postId, Long viewerId) {
        List<Comment> tops = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId);
        List<CommentDTO> out = new ArrayList<>();
        for (Comment t : tops) {
            List<CommentDTO> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(t.getId())
                    .stream()
                    .map(r -> CommentDTO.from(r, liked(r.getId(), viewerId)))
                    .toList();
            out.add(CommentDTO.from(t, liked(t.getId(), viewerId), replies));
        }
        return out;
    }

    /** 评论点赞 / 取消点赞（按用户幂等） */
    @Transactional
    public CommentDTO toggleCommentLike(Long commentId, Long userId) {
        if (userId == null) throw new IllegalStateException("请先登录后再点赞");
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在: " + commentId));
        boolean nowLiked;
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
            nowLiked = false;
        } else {
            commentLikeRepository.save(new CommentLike(commentId, userId));
            c.setLikeCount(c.getLikeCount() + 1);
            nowLiked = true;
        }
        commentRepository.save(c);
        return CommentDTO.from(c, nowLiked);
    }

    /**
     * 发表评论：parentId 为空表示顶层评论，否则为回复。
     * 「回复的回复」归入同一顶层（保持单层嵌套，避免无限层）。
     * 会向帖子作者 / 被回复者 / 被 @提及 的用户生成通知。
     */
    @Transactional
    public CommentDTO addComment(Long postId, Long authorId, String content, Long parentId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + postId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + authorId));
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("评论内容不能为空");

        Comment target = null;
        Long topId = null;
        if (parentId != null) {
            target = commentRepository.findById(parentId).orElse(null);
            if (target != null) {
                topId = target.getParentId() != null ? target.getParentId() : target.getId();
            }
        }

        Comment c = new Comment(post, author, text, LocalDateTime.now());
        c.setParentId(topId);
        c = commentRepository.save(c);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 通知：回复被回复者；否则通知帖子作者
        if (target != null) {
            notificationService.notify(target.getAuthor().getId(), "REPLY", authorId, author.getName(),
                    "POST", postId, "回复了你的评论");
        } else {
            notificationService.notify(post.getAuthor().getId(), "COMMENT", authorId, author.getName(),
                    "POST", postId, "评论了你的帖子");
        }
        // 通知：正文 @提及 的用户
        notifyMentions(text, authorId, author.getName(), "POST", postId);

        return CommentDTO.from(c, false);
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

    private boolean liked(Long commentId, Long viewerId) {
        return viewerId != null && commentLikeRepository.existsByCommentIdAndUserId(commentId, viewerId);
    }
}
