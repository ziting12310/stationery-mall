package com.stationerymall.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 游标分页响应 VO（对齐 api-spec 1.5 节：{@code {items, nextCursor, hasMore}}）。
 * <p>
 * 约定：Service 按 {@code size + 1} 条查询，多出 1 条即代表还有下一页；
 * 由 {@link #of(List, int, Function)} 统一裁剪并计算 {@code nextCursor}，
 * 避免每个列表接口各写一遍分页逻辑。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> items;

    /** 下一页游标（最后一条 id）；为 null 表示没有下一页 */
    private Long nextCursor;

    /** 是否还有下一页 */
    private Boolean hasMore;

    /**
     * 由「多查 1 条」的结果组装分页对象。
     *
     * @param rows     查询结果（条数应为 pageSize + 1）
     * @param pageSize 请求页大小
     * @param idGetter 从元素取 id，用于计算 nextCursor
     */
    public static <T> PageVO<T> of(List<T> rows, int pageSize, Function<T, Long> idGetter) {
        PageVO<T> page = new PageVO<>();
        boolean hasMore = rows.size() > pageSize;
        List<T> items = hasMore ? new ArrayList<>(rows.subList(0, pageSize)) : rows;
        page.setItems(items);
        page.setHasMore(hasMore);
        page.setNextCursor(hasMore && !items.isEmpty()
                ? idGetter.apply(items.get(items.size() - 1)) : null);
        return page;
    }

    /** 空页 */
    public static <T> PageVO<T> empty() {
        PageVO<T> page = new PageVO<>();
        page.setItems(List.of());
        page.setHasMore(false);
        page.setNextCursor(null);
        return page;
    }
}
