package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞记录（按用户持久化）。
 * 一条记录表示「某用户对某帖子点过赞」，配合 Post.likeCount 这一非规范化计数使用：
 * - likeCount 作为展示用的总点赞数（种子数据自带，新点赞在原基础上 ±1）；
 * - 本表仅用于判断「当前用户是否已点赞」，避免重复点赞 / 幂等取消。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_post_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_user", columnNames = {"post_id", "user_id"}))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}
