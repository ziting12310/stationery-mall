package com.stationerymall.service.impl;

import com.stationerymall.dto.CommentSubmitDTO;
import com.stationerymall.entity.Comment;
import com.stationerymall.entity.Order;
import com.stationerymall.entity.OrderItem;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.ParamInvalidException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.mapper.CommentMapper;
import com.stationerymall.mapper.OrderItemMapper;
import com.stationerymall.mapper.OrderMapper;
import com.stationerymall.service.CommentService;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.CommentVO;
import com.stationerymall.vo.EvaluationStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价服务实现。
 * <p>
 * 规则：订单属于当前用户 + 订单状态 COMPLETED + 商品在订单明细中 +
 * 同一订单同一商品未评价过（UNIQUE(order_id, product_id, user_id) 的应用层前置校验）。
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 重复评价提示语（前端据此展示，避免重复提交） */
    private static final String DUPLICATE_EVALUATION_MSG = "你已提交评价";

    private final CommentMapper commentMapper;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    @Override
    public CommentVO submit(CommentSubmitDTO dto) {
        Long userId = requireNormalUser();

        // 1. 订单存在且属于当前用户
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权评价他人订单");
        }

        // 2. 仅已完成订单可评价
        if (!order.getStatus().canEvaluate()) {
            throw new BusinessStateException("订单未完成，无法评价，当前状态：" + order.getStatus().getDesc());
        }

        // 3. 被评商品必须存在于该订单明细中
        List<OrderItem> items = orderItemMapper.selectByOrderId(dto.getOrderId());
        boolean inOrder = items.stream()
                .anyMatch(item -> dto.getProductId().equals(item.getProductId()));
        if (!inOrder) {
            throw new ParamInvalidException("该商品不在订单中，无法评价");
        }

        // 4. 按「订单 + 商品」校验是否已评价：已存在直接提示，不再重复保存（409）
        if (commentMapper.countByOrderProductUser(dto.getOrderId(), dto.getProductId(), userId) > 0) {
            throw new BusinessStateException(BizCodeEnum.CONFLICT, DUPLICATE_EVALUATION_MSG);
        }

        Comment comment = new Comment();
        comment.setOrderId(dto.getOrderId());
        comment.setProductId(dto.getProductId());
        comment.setUserId(userId);
        comment.setRating(dto.getRating());
        comment.setContent(dto.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        try {
            commentMapper.insert(comment);
        } catch (DuplicateKeyException e) {
            // 并发双击兜底：唯一键 (order_id, product_id, user_id) 冲突时同样返回「你已提交评价」
            throw new BusinessStateException(BizCodeEnum.CONFLICT, DUPLICATE_EVALUATION_MSG);
        }

        return toVO(comment);
    }

    @Override
    public List<CommentVO> listByProductId(Long productId) {
        return commentMapper.selectByProductId(productId).stream().map(this::toVO).toList();
    }

    @Override
    public EvaluationStatusVO getStatus(Long orderId, Long productId, Long userId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new UnauthException();
        }

        // 管理员查看：只用 orderId + productId 查询该订单商品的评价（不拼接管理员 user_id）
        if (UserContext.getRole().isAdmin()) {
            return toStatusVO(commentMapper.selectByOrderProduct(orderId, productId));
        }

        // 普通用户：必须携带 userId，且只能查询自己提交的评价
        Long queryUserId = (userId == null || userId <= 0) ? currentUserId : userId;
        if (!currentUserId.equals(queryUserId)) {
            throw new ForbiddenException("无权查看他人评价");
        }
        return toStatusVO(commentMapper.selectByOrderProductUser(orderId, productId, queryUserId));
    }

    /** 评价记录 → 状态 VO（null 表示未评价） */
    private EvaluationStatusVO toStatusVO(Comment comment) {
        EvaluationStatusVO vo = new EvaluationStatusVO();
        if (comment == null) {
            vo.setHasComment(false);
            return vo;
        }
        vo.setHasComment(true);
        vo.setContent(comment.getContent());
        return vo;
    }

    @Override
    public List<CommentVO> listEvaluations(Long userId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new UnauthException();
        }

        // 管理员：不拼接 user_id 条件，返回系统内全部用户评价
        if (UserContext.getRole().isAdmin()) {
            return commentMapper.selectList(null);
        }

        // 普通用户：必须携带 userId，且只能查询自己提交的评价
        if (userId == null) {
            throw new ParamInvalidException("普通用户查询评价必须携带 userId");
        }
        if (!currentUserId.equals(userId)) {
            throw new ForbiddenException("只能查看自己提交的评价");
        }
        return commentMapper.selectList(currentUserId);
    }

    /* ==================== 私有辅助 ==================== */

    /** 仅普通用户可评价：未登录 401；管理员 403 */
    private Long requireNormalUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthException();
        }
        if (!UserContext.getRole().canPlaceOrder()) {
            throw new ForbiddenException("管理员不能评价");
        }
        return userId;
    }

    private CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setProductId(comment.getProductId());
        vo.setUserId(comment.getUserId());
        vo.setRating(comment.getRating());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }
}
