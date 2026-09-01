package com.xueqiu.clone.service;

import com.xueqiu.clone.dto.CommentDTO;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.StockDTO;
import com.xueqiu.clone.model.Comment;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.Stock;
import com.xueqiu.clone.model.User;
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
import java.util.List;

/**
 * 个股服务：热门列表、详情、相关帖子、评论（分页）。
 */
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public StockService(StockRepository stockRepository, PostRepository postRepository,
                        CommentRepository commentRepository, UserRepository userRepository) {
        this.stockRepository = stockRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
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

    /** 评论分页（按时间正序，便于楼层展示） */
    public Page<CommentDTO> getComments(Long postId, int page, int size) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(page, size))
                .map(CommentDTO::from);
    }

    /** 发表评论：同时自增帖子评论数 */
    @Transactional
    public CommentDTO addComment(Long postId, Long authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在: " + postId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + authorId));
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("评论内容不能为空");
        Comment c = new Comment(post, author, text, LocalDateTime.now());
        c = commentRepository.save(c);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        return CommentDTO.from(c);
    }
}
