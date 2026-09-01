package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 帖子（雪球中的「动态 / 讨论」）。
 * 一篇帖子可关联多只股票标签，作者为一名用户。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime createdAt;

    private int likeCount;

    private int commentCount;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "t_post_stock",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "stock_id"))
    private List<Stock> stocks = new ArrayList<>();

    public Post(User author, String content, LocalDateTime createdAt,
                int likeCount, int commentCount, List<Stock> stocks) {
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.stocks = stocks;
    }
}
