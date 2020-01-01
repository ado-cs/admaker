package we.lcx.admaker.service.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.common.entities.Pair;
import we.lcx.admaker.service.Basic;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lin Chenxiao on 2019-12-31
 **/
@Aspect
@Component
public class TraceAop {
    private static final ConcurrentHashMap<String, TraceLog> data = new ConcurrentHashMap<>();

    @Resource
    private Basic basic;

    @Around("@annotation(trace)")
    public Object doAround(ProceedingJoinPoint pjp, Trace trace) throws Throwable {
        NewAds ad = null;
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof NewAds) {
                ad = ((NewAds) arg);
                break;
            }
        }
        if (ad == null) return null;
        TraceLog traceLog = data.computeIfAbsent(ad.getTraceId(), v -> new TraceLog());
        if (trace.main()) {
            ad.setAmount(ad.getAmount() - traceLog.finished);
            Result result = (Result) pjp.proceed(pjp.getArgs());
            if (result.getResults() == null) {
                data.remove(ad.getTraceId());
                return result;
            }
            Set adIds = (Set) result.getResults();
            traceLog.finished = adIds.size();
            Set<Integer> failed = basic.approveAds(ad, adIds);
            if (adIds.size() == ad.getAmount() && failed.size() == 0) {
                data.remove(ad.getTraceId());
                return Result.ok();
            }
            traceLog.failApproved = failed;
            return Result.fail(String.format("%d个广告单创建失败！", failed.size() + ad.getAmount() - adIds.size()));
        }
        else if (trace.approve()) {
            Set set = null;
            for (Object arg : pjp.getArgs()) {
                if (arg instanceof Set) {
                    set = ((Set) arg);
                    break;
                }
            }
            if (set == null) return null;
            if (!CollectionUtils.isEmpty(traceLog.failApproved)) set.addAll(traceLog.failApproved);
            return pjp.proceed(pjp.getArgs());
        }
        String name = trace.value();
        if (StringUtils.isEmpty(name)) name = pjp.getSignature().getName();
        Object r;
        if (trace.loop()) {
            Pair<Set<Object>, Set<Object>> pair = traceLog.group.computeIfAbsent(name, v -> new Pair<>());
            Set<Object> record = pair.getKey(v -> new HashSet<>());
            Set<Object> unused = pair.getValue(v -> new HashSet<>());
            if (unused.size() > 0) {
                r = unused.iterator().next();
                unused.remove(r);
            }
            else {
                r = pjp.proceed(pjp.getArgs());
                record.add(r);
                unused.add(r);
            }
        }
        else {
            r = traceLog.single.get(name);
            if (r == null) {
                r = pjp.proceed(pjp.getArgs());
                traceLog.single.put(name, r);
            }
        }
        return r;
    }

    public void use(String traceId, String name, Object r) {
        TraceLog traceLog = data.get(traceId);
        if (traceLog == null) return;
        Pair<Set<Object>, Set<Object>> pair = traceLog.group.get(name);
        if (pair == null) return;
        Set<Object> set = pair.getValue();
        if (set == null) return;
        set.remove(r);
    }

    //记录cancel要撤回的条目，合约为dealItem和预约，竞价为广告
    public void done(String traceId, Object obj) {
        TraceLog traceLog = data.get(traceId);
        if (traceLog == null) return;
        traceLog.cancelItems.add(obj);
    }

    public void done(String traceId, Collection collection) {
        TraceLog traceLog = data.get(traceId);
        if (traceLog == null) return;
        traceLog.cancelItems.addAll(collection);
    }

    public Set<Object> cancel(String traceId) {
        TraceLog traceLog = data.remove(traceId);
        return traceLog == null ? new HashSet<>() : traceLog.cancelItems;
    }

    private class TraceLog {
        HashMap<String, Object> single = new HashMap<>();
        HashMap<String, Pair<Set<Object>, Set<Object>>> group = new HashMap<>();
        Set<Object> cancelItems = new HashSet<>();
        int finished = 0;
        Set<Integer> failApproved;
    }
}
