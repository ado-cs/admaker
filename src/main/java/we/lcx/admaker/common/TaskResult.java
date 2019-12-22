package we.lcx.admaker.common;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Created by Lin Chenxiao on 2019-12-22
 **/
@Slf4j
public class TaskResult {
    private boolean success = false;
    private Entity entity;
    private HttpStatus status;
    private String body;

    public static TaskResult of(ResponseEntity<String> resp) {
        TaskResult result = new TaskResult();
        if (resp == null) return result;
        result.status = resp.getStatusCode();
        result.body = resp.getBody();
        Map map = JSON.parseObject(result.body, Map.class);
        if (map == null) return result;
        Object success = map.get("success");
        if (!(success instanceof Boolean && (Boolean) success)) return result;
        result.entity = new Entity(map);
        result.success = true;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void error() {
        if (success) return;
        log.error("entity invalid, code = {}, body = {}", status, body);
    }

    public void valid(String message) {
        if (success) return;
        log.error("entity invalid, code = {}, body = {}", status, body);
        throw new RuntimeException(message);
    }

    public Entity getEntity() {
        return entity;
    }
}
