package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 关注关系（社交图谱）。
 * 一条记录表示 follower 关注了 following；(follower_id, following_id) 唯一，避免重复关注。
 * 粉丝数 / 关注数由本表实时聚合（countByFollowingId / countByFollowerId），不再依赖 User.followers 静态字段。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_follow", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id")
    private User following;

    private LocalDateTime createdAt;

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
        this.createdAt = LocalDateTime.now();
    }
}
