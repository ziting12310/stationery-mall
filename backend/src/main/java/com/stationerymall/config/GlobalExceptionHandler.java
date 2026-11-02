package com.stationerymall.config;

import com.stationerymall.Result;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.ParamInvalidException;
import com.stationerymall.exception.StockNotEnoughException;
import com.stationerymall.exception.UnauthException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 把 5 类自定义业务异常映射到 {@link BizCodeEnum} 的具体 code，统一返回 {@code { code, msg, data }}，
 * HTTP 状态码与业务 code 保持一致，便于前端 Axios 拦截器判断。
 * <b>绝不向前端返回异常堆栈或原始异常类名。</b>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未登录 / 登录态失效 —— 401 */
    @ExceptionHandler(UnauthException.class)
    public ResponseEntity<Result<Void>> handleUnauth(UnauthException e) {
        log.debug("未登录或登录态失效：{}", e.getMessage());
        return build(e.getBizCode(), e.getMessage());
    }

    /** 无权限（角色不符 / 访问他人资源） —— 403 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<Void>> handleForbidden(ForbiddenException e) {
        log.warn("越权访问拦截：{}", e.getMessage());
        return build(e.getBizCode(), e.getMessage());
    }

    /** 库存不足 —— 400，事务需整单回滚 */
    @ExceptionHandler(StockNotEnoughException.class)
    public ResponseEntity<Result<Void>> handleStockNotEnough(StockNotEnoughException e) {
        log.info("库存校验失败：{}", e.getMessage());
        return build(e.getBizCode(), e.getMessage());
    }

    /** 业务状态不合法（订单状态流转越权等） —— 400 */
    @ExceptionHandler(BusinessStateException.class)
    public ResponseEntity<Result<Void>> handleBusinessState(BusinessStateException e) {
        log.info("业务状态校验失败：{}", e.getMessage());
        return build(e.getBizCode(), e.getMessage());
    }

    /** 业务参数校验失败 —— 400 */
    @ExceptionHandler(ParamInvalidException.class)
    public ResponseEntity<Result<Void>> handleParamInvalid(ParamInvalidException e) {
        log.info("参数校验失败：{}", e.getMessage());
        return build(e.getBizCode(), e.getMessage());
    }

    /** Jakarta Validation：@Valid 请求体校验失败 —— 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return build(BizCodeEnum.PARAM_INVALID,
                msg.isBlank() ? BizCodeEnum.PARAM_INVALID.getMsg() : msg);
    }

    /** Jakarta Validation：表单 / 查询参数绑定失败 —— 400 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return build(BizCodeEnum.PARAM_INVALID,
                msg.isBlank() ? BizCodeEnum.PARAM_INVALID.getMsg() : msg);
    }

    /** Jakarta Validation：方法参数级校验失败 —— 400 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        return build(BizCodeEnum.PARAM_INVALID, e.getMessage());
    }

    /** Query / Path 参数类型转换失败（如 status=ABC、size=abc） —— 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(BizCodeEnum.PARAM_INVALID, "参数 " + e.getName() + " 取值不合法");
    }

    /** 请求体 JSON 语法错误 / 字段类型不匹配 —— 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return build(BizCodeEnum.PARAM_INVALID, "请求体格式不正确，请检查 JSON 语法与字段类型");
    }

    /** 访问不存在的接口路径 —— 404（避免被 Exception 兜底成 500） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return build(BizCodeEnum.NOT_FOUND, "接口不存在");
    }

    /** 兜底未知异常 —— 500（仅服务端记录堆栈，前端只看到通用提示） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("服务器内部异常", e);
        return build(BizCodeEnum.INTERNAL_ERROR, BizCodeEnum.INTERNAL_ERROR.getMsg());
    }

    /* ==================== 统一构造 ==================== */

    private ResponseEntity<Result<Void>> build(BizCodeEnum bizCode, String msg) {
        HttpStatus status = resolveStatus(bizCode.getCode());
        return ResponseEntity.status(status).body(Result.error(bizCode, msg));
    }

    /**
     * 业务码 -> HTTP 状态码。
     * 业务码与 HTTP 状态同值；非标准 HTTP 码统一按 500 处理，避免出现非法状态。
     */
    private HttpStatus resolveStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
