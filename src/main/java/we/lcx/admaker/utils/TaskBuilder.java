package we.lcx.admaker.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.http.HttpMethod;
import we.lcx.admaker.common.Task;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LinChenxiao on 2019/12/12 19:04
 **/
public class TaskBuilder {
    private Task task = new Task();

    public static TaskBuilder get(String url) {
        TaskBuilder builder = post(url);
        builder.task.setMethod(HttpMethod.GET);
        return builder;
    }

    public static TaskBuilder post(String url) {
        TaskBuilder builder = new TaskBuilder();
        builder.task.setUrl(url);
        return builder;
    }

    public TaskBuilder cookie(String cookie) {
        task.setCookie(cookie);
        return this;
    }

    @SuppressWarnings("unchecked")
    public TaskBuilder param(String params) {
        task.setParams(JSON.parseObject(params, Map.class));
        return this;
    }

    @SuppressWarnings("unchecked")
    public TaskBuilder param(String key, Object value) {
        if (task.getParams() == null) task.setParams(new HashMap<>());
        if (key != null && key.indexOf(' ') != -1) {
            String[] keys = key.split(" ");
            Map map = Helper.getMap(task.getParams(), keys);
            if (map != null) map.put(keys[keys.length - 1], value);
        }
        else task.getParams().put(key, value);
        return this;
    }

    public Task build() {
        return task;
    }
}
