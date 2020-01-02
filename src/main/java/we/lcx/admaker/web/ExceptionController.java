package we.lcx.admaker.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.VisibleException;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Lin Chenxiao on 2019-09-14
 **/
@Slf4j
@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(value = Exception.class)
    public Result handle(Exception e, HttpServletRequest request) {
        if (!(e instanceof VisibleException)) {
            log.error("e={}, message={}, cause={}", e.getClass(), e.getMessage(), e.getCause());
            if (e.getMessage() == null && e.getCause() == null) e.printStackTrace();
        }
        return Result.fail(e.getMessage() == null ? "未知错误" : e.getMessage(), request.getAttribute("traceId"));
    }
}
