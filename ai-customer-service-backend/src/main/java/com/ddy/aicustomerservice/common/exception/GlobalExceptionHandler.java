package com.ddy.aicustomerservice.common.exception;

import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 *
 * 作用：
 * 1. 统一处理异常，所有接口返回 Result 格式
 * 2. 业务异常按 DEBUG 输出，避免污染线上日志
 * 3. 系统异常打印完整堆栈，便于排查
 * 4. 不向前端透露异常栈
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(HttpServletRequest request, BusinessException e) {
        log.warn("[业务异常] {} {} code={} msg={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");

        log.warn("[参数校验失败] {}", message);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 表单参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数绑定失败");

        log.warn("[参数绑定失败] {}", message);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 单个参数校验异常
     *
     * 例如 @RequestParam @NotBlank String keyword
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[参数校验失败] {}", e.getMessage());
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[请求体格式错误] {}", e.getMessage());
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "请求参数格式错误");
    }

    /**
     * 上传文件过大
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[上传文件过大] {}", e.getMessage());
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "上传文件大小超过限制");
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException e) {
        log.warn("[权限不足] {} {}", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCodeEnum.FORBIDDEN.getCode(), "没有权限访问");
    }

    /**
     * 兜底异常
     *
     * 这里要打印完整堆栈，便于线上排查。
     * 但不能把堆栈返回给前端。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(HttpServletRequest request, Exception e) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);

        return Result.fail(
                ResultCodeEnum.FAIL.getCode(),
                "系统异常，请稍后再试"
        );
    }
}
