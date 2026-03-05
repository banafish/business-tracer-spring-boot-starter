package com.bananice.businesstracer.presentation.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装类
 * JSON 序列化结果与原有 Map 格式完全一致: {"code":200,"data":...,"message":"..."}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private int code;
    private T data;
    private String message;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, data, null);
    }

    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(200, data, message);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, null, message);
    }
}
