package com.xueqiu.clone.controller;

import com.xueqiu.clone.model.IndexDef;
import com.xueqiu.clone.repository.IndexDefRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指数配置管理接口（可配置落库）。
 * 读取类 GET 对所有访客开放；写操作需登录（见 SecurityConfig 的 anyRequest().authenticated()）。
 * 前端「指数配置」管理等 UI 可据此增删改，改动即时反映到侧栏指数模块与 WebSocket 推送。
 */
@RestController
@RequestMapping("/api/index-defs")
public class IndexDefController {

    private final IndexDefRepository indexDefRepository;

    public IndexDefController(IndexDefRepository indexDefRepository) {
        this.indexDefRepository = indexDefRepository;
    }

    /** 全部指数配置（含停用项），用于管理界面 */
    @GetMapping
    public List<IndexDef> list() {
        return indexDefRepository.findAllByOrderBySortOrderAsc();
    }

    /** 新增指数配置 */
    @PostMapping
    public IndexDef create(@RequestBody IndexDef def) {
        if (def.getCode() == null || def.getCode().isBlank())
            throw new IllegalArgumentException("code 不能为空");
        if (def.getName() == null || def.getName().isBlank())
            throw new IllegalArgumentException("name 不能为空");
        def.setId(null);
        if (!def.isEnabled()) def.setEnabled(true);
        return indexDefRepository.save(def);
    }

    /** 更新指数配置（名称 / 市场 / 顺序 / 启用状态） */
    @PutMapping("/{id}")
    public IndexDef update(@PathVariable Long id, @RequestBody IndexDef def) {
        IndexDef existing = indexDefRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("指数配置不存在: " + id));
        if (def.getCode() != null) existing.setCode(def.getCode());
        if (def.getName() != null) existing.setName(def.getName());
        if (def.getMarket() != null) existing.setMarket(def.getMarket());
        existing.setSortOrder(def.getSortOrder());
        existing.setEnabled(def.isEnabled());
        return indexDefRepository.save(existing);
    }

    /** 删除指数配置 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        indexDefRepository.deleteById(id);
    }
}
