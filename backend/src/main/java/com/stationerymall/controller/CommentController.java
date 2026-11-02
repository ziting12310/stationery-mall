package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.dto.CommentSubmitDTO;
import com.stationerymall.service.CommentService;
import com.stationerymall.vo.CommentVO;
import com.stationerymall.vo.EvaluationStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评价接口 {@code /api/evaluations}。
 * <p>
 * 提交评价仅普通用户可用；评价列表管理员可查看全部用户、普通用户仅限本人。
 * 订单归属、订单状态、商品是否在订单中、是否重复评价均由 Service 校验。
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 提交评价：POST /api/evaluations（仅已完成订单中的商品，每单每商品一次） */
    @PostMapping
    public Result<CommentVO> submit(@Valid @RequestBody CommentSubmitDTO dto) {
        return Result.success(commentService.submit(dto));
    }

    /**
     * 评价列表：GET /api/evaluations?userId=xxx
     * <p>管理员忽略 userId，返回全部用户评价（含用户 ID、订单号、商品名称、评价内容、创建时间）；
     * 普通用户必须携带 userId 且只能查询本人评价。
     */
    @GetMapping
    public Result<List<CommentVO>> list(@RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(commentService.listEvaluations(userId));
    }

    /**
     * 评价状态：GET /api/evaluations/status?orderId=xxx&amp;productId=xxx&amp;userId=xxx
     * <p>返回 {@code {hasComment, content}}：已评价时 hasComment=true 并带回评价文字。
     */
    @GetMapping("/status")
    public Result<EvaluationStatusVO> status(@RequestParam("orderId") Long orderId,
                                             @RequestParam("productId") Long productId,
                                             @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(commentService.getStatus(orderId, productId, userId));
    }
}
