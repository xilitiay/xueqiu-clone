package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.UserProfileDTO;
import com.xueqiu.clone.service.UserService;
import org.springframework.web.bind.annotation.*;

/** 用户主页接口。 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}")
    public UserProfileDTO user(@PathVariable Long id) {
        return userService.getProfile(id);
    }
}
