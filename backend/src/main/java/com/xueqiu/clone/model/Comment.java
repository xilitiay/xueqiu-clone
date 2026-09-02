package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论（帖子下的回复）。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 嵌套回复：顶层评论 parentId 为 null；回复某条评论时指向其所属顶层评论 id（保持单层） */
    private Long parentId;

    /** 评论点赞数（非规范化计数，配合 CommentLike 表判断当前用户是否点赞） */
    private int likeCount = 0;

    private LocalDateTime createdAt;

    public Comment(Post post, User author, String content, LocalDateTime createdAt) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }
}
