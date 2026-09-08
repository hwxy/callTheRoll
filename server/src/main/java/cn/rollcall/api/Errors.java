package cn.rollcall.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(Problem.class)
    public ResponseEntity<?> problem(Problem e) {
        return response(e.status, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return response(400, e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).findFirst().orElse("参数错误"));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> badInput(Exception e) {
        return response(400, "请求参数格式不正确");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> duplicate(Exception e) {
        return response(409, "登录标识已存在或请求重复，请刷新后重试");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied(Exception e) {
        return response(403, "无权执行此操作");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> database(Exception e) {
        return response(503, "数据服务暂不可用，请稍后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> upload(Exception e) {
        return response(413, "文件大小不能超过 2MB");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(Errors.class).error("Unhandled request error: {}", e.getClass().getName());
        return response(500, "操作未完成，请稍后重试");
    }

    private ResponseEntity<?> response(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("code", status, "message", message));
    }
}
