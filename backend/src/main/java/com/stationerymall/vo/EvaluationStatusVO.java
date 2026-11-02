package com.stationerymall.vo;

import lombok.Data;

/**
 * 评价状态响应 VO（订单页读取「某订单 + 某商品 + 某用户」是否已评价）。
 */
@Data
public class EvaluationStatusVO {

    /** 是否已提交评价 */
    private Boolean hasComment;

    /** 已评价时的评价文字（未评价为 null） */
    private String content;
}
