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
        Result result = new Result();
        result.success = true;
        result.results = results;
        return result;
    }

    public static Result fail(String message) {
        Result result = new Result();
        result.success = false;
        result.message = message;
        return result;
    }
}
