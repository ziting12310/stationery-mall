package com.stationerymall.service;

import com.stationerymall.dto.CommentSubmitDTO;
import com.stationerymall.vo.CommentVO;
import com.stationerymall.vo.EvaluationStatusVO;

import java.util.List;

/**
 * 评价服务（仅普通用户）。
 * <p>
 * 规则：订单必须属于当前用户且状态为 COMPLETED；商品必须在该订单明细中；
 * 同一订单 + 同一商品 + 同一用户只能评价一次。
 */
public interface CommentService {

    /** 提交评价 */
    CommentVO submit(CommentSubmitDTO dto);

    /** 商品评价列表（公开，商品详情页使用） */
    List<CommentVO> listByProductId(Long productId);

    /** 评价状态（订单页使用）：按 orderId + productId + userId 查询是否已评价及评价文字 */
    EvaluationStatusVO getStatus(Long orderId, Long productId, Long userId);

    /**
     * 评价列表：管理员查看全部用户评价（忽略 userId）；普通用户必须携带 userId 且只能是本人。
     */
    List<CommentVO> listEvaluations(Long userId);
}
