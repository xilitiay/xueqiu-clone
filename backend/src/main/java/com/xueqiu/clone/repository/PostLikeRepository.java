package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /** 判断某用户是否已对某帖子点赞 */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /** 删除某用户对某帖子的点赞（用于取消点赞） */
    void deleteByPostIdAndUserId(Long postId, Long userId);

    /** 某用户累计点赞数（可选，用于「我的点赞」统计） */
    long countByUserId(Long userId);
}
