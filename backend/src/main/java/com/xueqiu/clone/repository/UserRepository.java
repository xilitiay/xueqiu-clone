package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<User> findByUsername(String username);

    /** 搜索：昵称包含关键字（忽略大小写） */
    java.util.List<User> findByNameContainingIgnoreCase(String name);
}
