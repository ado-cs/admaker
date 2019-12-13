package we.lcx.admaker.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;

/**
 * Created by LinChenxiao on 2019/12/12 19:03
 **/
public class Result {
    private ResponseEntity<String> entity;

    public static Result of (ResponseEntity<String> entity) {
        Result result = new Result();
        result.entity = entity;
        return result;
    }

    public boolean isSuccess() {
        return entity.getStatusCode() == HttpStatus.OK;
    }

    public HttpHeaders getHeader() {
        return entity.getHeaders();
    }

    public List<String> getCookie() {
        HttpHeaders headers = entity.getHeaders();
        return headers == null ? null : headers.get("Cookie");
    }

    public String getBody() {
        return entity.getBody();
    }
}
