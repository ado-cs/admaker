package we.lcx.admaker.service.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.Result;
import we.lcx.admaker.common.entities.NewAds;
import we.lcx.admaker.service.BasicService;
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
    private BasicService basicService;

    @Around("execution (* we.lcx.admaker.manager.impl.*.create(..))")
    public Object doAroundMain(ProceedingJoinPoint pjp) throws Throwable {
        NewAds ad = (NewAds) pjp.getArgs()[0];
        TraceLog traceLog = data.computeIfAbsent(ad.getTraceId(), v -> new TraceLog());
        ad.convert();
        traceLog.ads = ad;
        ad.setRealAmount(ad.getAmount() - traceLog.finished);
        Result result = (Result) pjp.proceed(pjp.getArgs());
        if (result.getResults() == null) {
            data.remove(ad.getTraceId());
            return result;
        }
        Set adIds = (Set) result.getResults();
        traceLog.finished += adIds.size();
        if (ad.getType() == 2 && !CollectionUtils.isEmpty(adIds)) traceLog.cancelItems.addAll(adIds);
        if (!CollectionUtils.isEmpty(traceLog.failApproved)) adIds.addAll(traceLog.failApproved);
        Set<Integer> failed = basicService.approveAds(ad, adIds);
        if (traceLog.finished == ad.getAmount() && failed.size() == 0) {
            data.remove(ad.getTraceId());
            return Result.ok();
        }
        traceLog.failApproved = failed;
        return Result.fail(String.format("%d个广告单创建失败！", failed.size() + ad.getAmount() - traceLog.finished), ad.getTraceId());
    }

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
        String name = trace.value();
        if (StringUtils.isEmpty(name)) name = pjp.getSignature().getName();
        Object r;
        if (trace.loop()) {
            Set<Object> unused = traceLog.group.computeIfAbsent(name, v -> new HashSet<>());
            if (unused.size() > 0) {
                r = unused.iterator().next();
                unused.remove(r);
            } else {
                r = pjp.proceed(pjp.getArgs());
                unused.add(r);
                if (ad.getType() == 1) traceLog.cancelItems.add(r);
            }
        } else {
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
        Set<Object> unused = traceLog.group.get(name);
        if (unused == null) return;
        unused.remove(r);
    }

    public List<Object> cancel(String traceId) {
        TraceLog traceLog = data.remove(traceId);
        return traceLog == null ? new ArrayList<>() : traceLog.cancelItems;
    }

    public NewAds getAd(String traceId) {
        TraceLog traceLog = data.get(traceId);
        return traceLog == null ? null : traceLog.ads;
    }

    private class TraceLog {
        NewAds ads;
        HashMap<String, Object> single = new HashMap<>();
        HashMap<String, Set<Object>> group = new HashMap<>();
        List<Object> cancelItems = new ArrayList<>();
        int finished = 0;
        Set<Integer> failApproved;
    }
}
