package com.xueqiu.clone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 雪球风格投资社区 - 后端启动类。
 * 技术栈：Java 17 + Spring Boot 3 + Spring Data JPA + H2（演示用内存库）。
 * 生产环境可平滑替换为 MySQL / Redis / 微服务拆分（与雪球真实架构一致）。
 */
@SpringBootApplication
@EnableScheduling
public class CloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloneApplication.class, args);
    }
}
