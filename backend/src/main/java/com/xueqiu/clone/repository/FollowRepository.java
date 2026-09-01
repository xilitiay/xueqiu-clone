package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** 我关注了哪些人（用于关注流、关注列表） */
    List<Follow> findByFollowerId(Long followerId);

    /** 谁关注了我（粉丝列表） */
    List<Follow> findByFollowingId(Long followingId);

    /** 是否已关注 */
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /** 取消关注 */
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /** 粉丝数 */
    long countByFollowingId(Long followingId);

    /** 关注数 */
    long countByFollowerId(Long followerId);
}
