package com.xueqiu.clone.service;

import com.xueqiu.clone.config.JwtUtil;
import com.xueqiu.clone.dto.AuthRequest;
import com.xueqiu.clone.dto.AuthResponse;
import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.RegisterRequest;
import com.xueqiu.clone.dto.UserDTO;
import com.xueqiu.clone.dto.UserProfileDTO;
import com.xueqiu.clone.model.Follow;
import com.xueqiu.clone.model.Post;
import com.xueqiu.clone.model.User;
import com.xueqiu.clone.repository.FollowRepository;
import com.xueqiu.clone.repository.PostRepository;
import com.xueqiu.clone.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务：主页资料、注册、登录、按用户名查询、关注关系。
 * 密码使用 BCrypt 哈希存储，令牌由 JwtUtil 签发。
 * 关注关系（Follow 表）驱动粉丝/关注计数、关注流与「是否已关注」状态。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FollowRepository followRepository;

    public UserService(UserRepository userRepository, PostRepository postRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.followRepository = followRepository;
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

    /** 用户主页：资料 + 其发布的帖子（按时间倒序）。无浏览者时 isFollowing=false */
    public UserProfileDTO getProfile(Long id) {
        return getProfile(id, null);
    }

    /** 用户主页（带浏览者视角）：聚合真实粉丝/关注数，并计算 isFollowing */
    public UserProfileDTO getProfile(Long id, Long viewerId) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        List<PostDTO> posts = postRepository
                .findByAuthorIdOrderByCreatedAtDesc(id, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(p -> PostDTO.from(p, false))
                .toList();
        int followers = (int) followRepository.countByFollowingId(id);
        int following = (int) followRepository.countByFollowerId(id);
        boolean isFollowing = viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, id);
        return UserProfileDTO.from(u, posts, followers, following, isFollowing);
    }

    /** 关注 / 取消关注（按用户幂等）。返回操作后的「是否关注」状态 */
    public boolean toggleFollow(Long followerId, Long targetId) {
        if (followerId.equals(targetId)) throw new IllegalArgumentException("不能关注自己");
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + followerId));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + targetId));
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, targetId);
            return false;
        }
        followRepository.save(new Follow(follower, target));
        return true;
    }

    /** 当前用户是否关注了 targetId */
    public boolean isFollowing(Long followerId, Long targetId) {
        if (followerId == null) return false;
        return followRepository.existsByFollowerIdAndFollowingId(followerId, targetId);
    }

    /** 某用户关注的人（用于关注列表 / 关注流筛选） */
    public List<Long> followingIds(Long viewerId) {
        return followRepository.findByFollowerId(viewerId).stream()
                .map(f -> f.getFollowing().getId())
                .toList();
    }

    /** 关注列表（UserDTO） */
    public List<UserDTO> listFollowing(Long id) {
        return followRepository.findByFollowerId(id).stream()
                .map(f -> UserDTO.from(f.getFollowing()))
                .toList();
    }

    /** 粉丝列表（UserDTO） */
    public List<UserDTO> listFollowers(Long id) {
        return followRepository.findByFollowingId(id).stream()
                .map(f -> UserDTO.from(f.getFollower()))
                .toList();
    }

    /** 按 id 或用户名解析用户 id（支持 /user/:id 与 /user/:username 两种入口） */
    public Long resolveUserId(String idOrName) {
        try {
            return Long.parseLong(idOrName);
        } catch (NumberFormatException e) {
            return userRepository.findByUsername(idOrName)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + idOrName))
                    .getId();
        }
    }
}
