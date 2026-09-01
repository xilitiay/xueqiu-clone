package com.xueqiu.clone.repository;

import com.xueqiu.clone.model.IndexDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndexDefRepository extends JpaRepository<IndexDef, Long> {

    /** 仅取启用的指数，按展示顺序排序 */
    List<IndexDef> findByEnabledTrueOrderBySortOrderAsc();

    /** 全部配置，按展示顺序排序（管理界面用） */
    List<IndexDef> findAllByOrderBySortOrderAsc();
}
