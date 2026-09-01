package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
}
