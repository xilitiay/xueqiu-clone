package com.xueqiu.clone.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指数配置（可配置落库）。
 * 侧栏「市场指数」模块展示哪些指数、顺序、是否启用，均由本表驱动，
 * 取代原先写死在配置里的逗号分隔字符串。新增 / 调整顺序 / 启停均通过接口维护。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_index_def")
public class IndexDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指数代码，如 SH000001（与行情源约定一致） */
    @Column(nullable = false, unique = true)
    private String code;

    /** 指数名称，如 上证指数 */
    @Column(nullable = false)
    private String name;

    /** 市场，如 沪市 / 港股 / 美股 */
    private String market;

    /** 展示顺序，越小越靠前 */
    private int sortOrder;

    /** 是否启用（停用后不再出现在指数行情中） */
    private boolean enabled = true;

    public IndexDef(String code, String name, String market, int sortOrder) {
        this.code = code;
        this.name = name;
        this.market = market;
        this.sortOrder = sortOrder;
        this.enabled = true;
    }
}
