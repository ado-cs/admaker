package we.lcx.admaker.utils;

import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by LinChenxiao on 2019/12/12 17:21
 **/
public class HttpExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static boolean execute(List<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) return false;
        List<CompletableFuture<Result>> futureList = new ArrayList<>();
        for (Task task : tasks) {
            futureList.add(CompletableFuture.supplyAsync(() -> doRequest(task), executor));
        }
        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get();
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static Result doRequest(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        if (!StringUtils.isEmpty(task.getCookie())) {
            List<String> cookie = new ArrayList<>();
            cookie.add(task.getCookie());
            headers.put(HttpHeaders.COOKIE, cookie);
        }
        Map params = null;
        if (!CollectionUtils.isEmpty(task.getParams())) {
            StringBuilder s = new StringBuilder();
            if (task.getMethod() == HttpMethod.GET) {
                for (Map.Entry<String, Object> entry : task.getParams().entrySet()) {
                    s.append('&').append(entry.getKey()).append('=');
                    if (entry.getValue() == null) s.append("null");
                    else s.append('"').append(entry.getValue().toString()).append('"');
                }
                task.setUrl(task.getUrl() + "?" + s.substring(1));
            }
            else {
                params = task.getParams();
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }
        HttpEntity<Map> httpEntity = new HttpEntity(params, headers);
        return Result.of(restTemplate.exchange(task.getUrl(), task.getMethod(), httpEntity, String.class));
    }
}
