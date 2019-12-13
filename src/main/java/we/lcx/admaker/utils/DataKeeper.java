package we.lcx.admaker.utils;

import we.lcx.admaker.common.Cache;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by LinChenxiao on 2019/12/13 10:30
 **/
public class DataKeeper {
    private static final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();


    public static void putCache(String key, Object value, long ttl) {
        if (key == null || value == null) return;
        Cache cache = new Cache();
        cache.setData(value);
        cache.setExpired(System.currentTimeMillis() + ttl);
        caches.put(key, cache);
    }

    public static Object getCache(String key) {
        Cache cache = caches.get(key);
        if (cache == null) return null;
        if (cache.getExpired() > System.currentTimeMillis()) {
            caches.remove(key);
            return null;
        }
        return cache.getData();
    }
}
