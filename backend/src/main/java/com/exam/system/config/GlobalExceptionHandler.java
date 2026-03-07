package com.exam.system.config;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException manve) {
            message = manve.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ":" + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "参数校验失败";
            }
        }
        return body(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleServerError(Exception ex, HttpServletRequest request) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", request.getRequestURI());
    }

    private Map<String, Object> body(HttpStatus status, String message, String path) {
        return Map.of(
                "code", status.value(),
                "error", message == null || message.isBlank() ? status.getReasonPhrase() : message,
                "path", path,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
