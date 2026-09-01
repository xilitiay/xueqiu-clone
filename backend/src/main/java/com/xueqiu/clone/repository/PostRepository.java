package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 信息流：按发布时间倒序分页 */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 某只股票相关的帖子（通过关联表） */
    @Query("select p from Post p join p.stocks s where s.symbol = :symbol order by p.createdAt desc")
    Page<Post> findByStockSymbol(@Param("symbol") String symbol, Pageable pageable);

    /** 某位用户发布的帖子（按时间倒序） */
    List<Post> findByAuthorIdOrderByCreatedAtDesc(@Param("authorId") Long authorId, Sort sort);

    /** 关注流：作者 id 在给定集合内的帖子（按时间倒序分页） */
    Page<Post> findByAuthorIdInOrderByCreatedAtDesc(@Param("authorIds") List<Long> authorIds, Pageable pageable);

    /** 搜索：内容包含关键字（忽略大小写），按时间倒序 */
    Page<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);
}
