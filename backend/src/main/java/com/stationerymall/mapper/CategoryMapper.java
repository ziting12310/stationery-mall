package com.stationerymall.mapper;

import com.stationerymall.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类表 {@code categories} 的纯数据访问接口（两级自关联）。
 * <p>
 * 约束：不判断「是否为二级分类」「同名是否重复」等业务规则，只提供查询与读写能力。
 */
@Mapper
public interface CategoryMapper {

    /** 新增分类；parentId 为 null 表示一级分类 */
    int insert(Category category);

    /** 按主键查询 */
    Category selectById(@Param("id") Long id);

    /** 查询全部分类（按 sort_order、id 升序） */
    List<Category> selectAll();

    /** 查询所有一级分类（parent_id IS NULL） */
    List<Category> selectLevelOne();

    /** 查询指定一级分类下的二级分类 */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 统计同级同名分类数量（parentId 为 null 时统计一级分类）。
     * 供上层判断「同级分类名唯一」。
     */
    int countByParentAndName(@Param("parentId") Long parentId, @Param("name") String name);

    /** 按主键动态更新名称 / 父级 / 排序 */
    int updateById(Category category);

    /** 按主键删除 */
    int deleteById(@Param("id") Long id);
}
