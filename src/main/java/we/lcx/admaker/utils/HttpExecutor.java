package we.lcx.admaker.utils;

import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by LinChenxiao on 2019/12/12 17:21
 **/
@Slf4j
public class HttpExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static List<Result> execute(List<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) return new ArrayList<>();
        List<CompletableFuture<Result>> futureList = new ArrayList<>();
        ConcurrentLinkedQueue<Result> results = new ConcurrentLinkedQueue<>();
        for (Task task : tasks) {
            futureList.add(CompletableFuture.supplyAsync(() -> doRequest(task), executor).whenComplete((v, e) -> {
                if (e != null) log.error("failed to execute, e={}", e);
                else if (v != null) results.add(v);
            }));
        }
        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get();
        }
        catch (Exception e) {
            log.error("failed to get future, e={}", e);
        }
        return new ArrayList<>(results);
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
