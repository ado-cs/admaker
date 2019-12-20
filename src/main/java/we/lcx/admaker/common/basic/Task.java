package we.lcx.admaker.common.basic;

import lombok.Data;
import org.springframework.http.HttpMethod;
import we.lcx.admaker.utils.WordsTool;
import java.util.List;
import java.util.Map;

/**
 * Created by LinChenxiao on 2019/12/12 19:02
 **/
@Data
public class Task {
    private String url;
    private HttpMethod method;
    private List<String> cookie;
    private Map params;

    public static Task get(String url) {
        Task task = new Task();
        task.url = url;
        task.method = HttpMethod.GET;
        return task;
    }

    public static Task post(String url) {
        Task task = new Task();
        task.url = url;
        task.method = HttpMethod.POST;
        return task;
    }

    public Task cookie(String cookie) {
        this.cookie = WordsTool.toList(cookie);
        return this;
    }

    public Task param(Entity entity) {
        this.params = entity.get();
        return this;
    }
}
