package com.xueqiu.clone.dto;

import java.math.BigDecimal;

/**
 * 市场指数视图（用于侧栏「市场指数」模块）。
 * 字段与前端 indices.json 约定保持一致：code / name / value / changePercent，
 * 这样后端接入实时数据后前端无需改动即可渲染。
 */
public record IndexDTO(String code, String name, BigDecimal value, BigDecimal changePercent) {
}
