package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /** 顶层评论（parentId 为 null），按时间正序 */
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    /** 某条评论下的回复，按时间正序 */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
