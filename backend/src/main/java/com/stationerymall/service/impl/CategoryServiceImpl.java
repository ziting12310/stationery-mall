package com.stationerymall.service.impl;

import com.stationerymall.entity.Category;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.mapper.CategoryMapper;
import com.stationerymall.service.CategoryService;
import com.stationerymall.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类服务实现（两级分类，只读）。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listTree() {
        List<Category> all = categoryMapper.selectAll();
        // 先按 id 建索引，再按 parentId 挂子树（一次查询完成两级组装）
        Map<Long, CategoryVO> byId = new HashMap<>(all.size());
        for (Category category : all) {
            byId.put(category.getId(), toVO(category));
        }

        List<CategoryVO> roots = new ArrayList<>();
        for (Category category : all) {
            CategoryVO vo = byId.get(category.getId());
            if (category.getParentId() == null) {
                roots.add(vo);
                continue;
            }
            CategoryVO parent = byId.get(category.getParentId());
            if (parent == null) {
                // 父分类缺失的脏数据也作为根节点返回，避免数据丢失
                roots.add(vo);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(vo);
        }
        return roots;
    }

    @Override
    public List<CategoryVO> listLevelOne() {
        return categoryMapper.selectLevelOne().stream().map(this::toVO).toList();
    }

    @Override
    public List<CategoryVO> listByParentId(Long parentId) {
        // 对齐 api-spec 3.2.2：parentId 必须是一级分类（parent_id IS NULL），否则 404
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null || parent.getParentId() != null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "一级分类不存在");
        }
        return categoryMapper.selectByParentId(parentId).stream().map(this::toVO).toList();
    }

    /** 实体转 VO（未填充 children，由调用方决定是否组装树） */
    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        return vo;
    }
}
