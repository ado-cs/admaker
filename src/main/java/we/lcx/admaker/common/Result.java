package we.lcx.admaker.common;

import lombok.Data;

/**
 * Created by LinChenxiao on 2019/12/12 19:03
 **/
@Data
public class Result {
    private Boolean success;
    private String message;
    private Object data;

    public static Result ok() {
        Result result = new Result();
        result.success = true;
        return result;
    }

    public static Result ok(Object data) {
        Result result = new Result();
        result.success = true;
        result.data = data;
        return result;
    }

    public static Result fail(String message) {
        Result result = new Result();
        result.success = false;
        result.message = message;
        return result;
    }

    public static Result fail(Object data) {
        Result result = new Result();
        result.success = false;
        result.data = data;
        return result;
    }
}
