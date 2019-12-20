package we.lcx.admaker.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import we.lcx.admaker.common.basic.Entity;
import we.lcx.admaker.common.basic.Task;

import java.util.*;
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

    public static List<Entity> execute(List<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) return new ArrayList<>();
        List<CompletableFuture<Entity>> futureList = new ArrayList<>();
        ConcurrentLinkedQueue<Entity> results = new ConcurrentLinkedQueue<>();
        for (Task task : tasks) {
            futureList.add(CompletableFuture.supplyAsync(() -> doRequest(task), executor).whenComplete((v, e) -> {
                if (e != null) log.error("failed to execute the task, e = {}", e);
                else if (v != null) results.add(v);
            }));
        }
        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            log.error("failed to get the future list, e={}", e);
        }
        return new ArrayList<>(results);
    }

    @SuppressWarnings("unchecked")
    public static Entity doRequest(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        if (!CollectionUtils.isEmpty(task.getCookie()))
            headers.put(HttpHeaders.COOKIE, task.getCookie());
        Map params = null;
        if (!CollectionUtils.isEmpty(task.getParams())) {
            if (task.getMethod() == HttpMethod.GET) {
                StringBuilder s = new StringBuilder();
                Set<Map.Entry> set = task.getParams().entrySet();
                for (Map.Entry entry : set) {
                    s.append('&').append(entry.getKey()).append('=');
                    if (entry.getValue() == null) s.append("null");
                    else s.append('"').append(entry.getValue().toString()).append('"');
                }
                task.setUrl(task.getUrl() + "?" + s.substring(1));
            } else {
                params = task.getParams();
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }
        return Entity.of(restTemplate.exchange(task.getUrl(), task.getMethod(), new HttpEntity(params, headers), String.class));
    }
}
