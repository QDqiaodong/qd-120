package com.stopper.asset.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Map<String, String> fieldErrors;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> validationError(String message, Map<String, String> fieldErrors) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMessage(message);
        result.setFieldErrors(fieldErrors);
        return result;
    }

    public static <T> Result<T> validationError(String field, String message) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMessage("参数校验失败");
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        result.setFieldErrors(errors);
        return result;
    }
}
