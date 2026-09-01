package com.xueqiu.clone.service;

import com.xueqiu.clone.config.JwtUtil;
import com.xueqiu.clone.dto.AuthRequest;
import com.xueqiu.clone.dto.AuthResponse;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.RegisterRequest;
import com.xueqiu.clone.dto.UserProfileDTO;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.User;
import com.xueqiu.clone.repository.PostRepository;
import com.xueqiu.clone.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务：主页资料、注册、登录、按用户名查询。
 * 密码使用 BCrypt 哈希存储，令牌由 JwtUtil 签发。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PostRepository postRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** 注册：用户名唯一，密码 BCrypt 哈希；注册成功直接返回登录令牌 */
    public AuthResponse register(RegisterRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().length() < 6) {
            throw new IllegalArgumentException("用户名与密码（至少6位）必填");
        }
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("用户名已被占用");
        }
        User u = new User(req.name() != null && !req.name().isBlank() ? req.name() : req.username(),
                "#E64340", "", 0);
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        u = userRepository.save(u);
        return login(new AuthRequest(req.username(), req.password()));
    }

    /** 登录：校验密码，签发 JWT */
    public AuthResponse login(AuthRequest req) {
        User u = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), u.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername());
        return new AuthResponse(token, "Bearer", u.getId(), u.getUsername(), u.getName(), u.getAvatarColor());
    }

    /** 按用户名查询（鉴权后还原当前用户用） */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
    }

    /** 用户主页：资料 + 其发布的帖子（按时间倒序） */
    public UserProfileDTO getProfile(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        List<PostDTO> posts = postRepository
                .findByAuthorIdOrderByCreatedAtDesc(id, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(p -> PostDTO.from(p, false))
                .toList();
        return UserProfileDTO.from(u, posts);
    }
}
