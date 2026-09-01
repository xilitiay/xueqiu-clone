package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户（雪球中的「球友」）。
 * avatarColor 用于前端用纯色头像占位，避免依赖外部图片资源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 登录用户名（唯一） */
    @Column(nullable = false, unique = true)
    private String username;

    /** 登录密码（BCrypt 哈希，绝不返回给前端） */
    @Column(nullable = false)
    private String password;

    /** 头像底色，如 #E64340 */
    private String avatarColor;

    private String bio;

    private int followers;

    public User(String name, String avatarColor, String bio, int followers) {
        this.name = name;
        this.avatarColor = avatarColor;
        this.bio = bio;
        this.followers = followers;
    }
}
