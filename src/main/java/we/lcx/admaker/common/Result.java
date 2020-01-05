package we.lcx.admaker.common;

import lombok.Data;

/**
 * Created by LinChenxiao on 2019/12/12 19:03
 **/
@Data
public class Result {
    private Boolean success;
    private String message;
    private Object results;

    public static Result ok() {
        Result result = new Result();
        result.success = true;
        return result;
    }

    public static Result ok(Object results) {
        Result result = ok();
        result.results = results;
        return result;
    }

    public static Result fail() {
        Result result = new Result();
        result.success = false;
        return result;
    }

    public static Result fail(String message) {
        Result result = fail();
        result.message = message;
        return result;
    }

    public static Result fail(String message, Object results) {
        Result result = fail(message);
        result.results = results;
        return result;
    }
}
