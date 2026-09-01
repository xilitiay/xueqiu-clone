package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.AuthRequest;
import com.xueqiu.clone.dto.AuthResponse;
import com.xueqiu.clone.dto.RegisterRequest;
import com.xueqiu.clone.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口（注册 / 登录），返回 JWT。
 * 这两个接口对所有匿名用户开放（见 SecurityConfig）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        return userService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        return userService.login(req);
    }
}
