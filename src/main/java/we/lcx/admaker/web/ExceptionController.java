package we.lcx.admaker.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import we.lcx.admaker.common.basic.Result;

/**
 * Created by Lin Chenxiao on 2019-09-14
 **/
@Slf4j
@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(value = Exception.class)
    public Result handle(Exception e) {
        log.error("ex={}, message={}, cause={}", e.getClass(), e.getMessage(), e.getCause());
        return Result.fail(e.getMessage());
    }
}
