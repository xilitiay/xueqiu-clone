package com.xueqiu.clone.controller;

import com.xueqiu.clone.dto.PostDTO;
import com.xueqiu.clone.dto.SearchResultDTO;
import com.xueqiu.clone.dto.StockBriefDTO;
import com.xueqiu.clone.dto.UserDTO;
import com.xueqiu.clone.repository.PostRepository;
import com.xueqiu.clone.repository.StockRepository;
import com.xueqiu.clone.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索接口：按关键字检索帖子（内容）、股票（名称/代码）、用户（昵称）。
 * type=all（默认）返回三类；也可指定 post / stock / user 之一。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int LIMIT = 20;

    private final PostRepository postRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    public SearchController(PostRepository postRepository, StockRepository stockRepository,
                            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public SearchResultDTO search(@RequestParam String q,
                                  @RequestParam(defaultValue = "all") String type) {
        String kw = q == null ? "" : q.trim();
        boolean all = "all".equalsIgnoreCase(type);

        List<PostDTO> posts = (all || "post".equalsIgnoreCase(type))
                ? postRepository.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(kw, PageRequest.of(0, LIMIT))
                    .map(p -> PostDTO.from(p, false))
                    .toList()
                : List.of();

        List<StockBriefDTO> stocks = (all || "stock".equalsIgnoreCase(type))
                ? stockRepository.findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(kw, kw).stream()
                    .map(StockBriefDTO::from)
                    .toList()
                : List.of();

        List<UserDTO> users = (all || "user".equalsIgnoreCase(type))
                ? userRepository.findByNameContainingIgnoreCase(kw).stream()
                    .map(UserDTO::from)
                    .toList()
                : List.of();

        return new SearchResultDTO(posts, stocks, users);
    }
}
