package we.lcx.admaker.common.basic;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.annotation.Ignore;
import we.lcx.admaker.common.annotation.Level;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by LinChenxiao on 2019/12/20 12:03
 **/
@Slf4j
public class Entity {
    private boolean valid = false;
    private ResponseEntity<String> resp;
    private Map content;

    public static Entity of(String param) {
        Entity entity = new Entity();
        if (StringUtils.isEmpty(param)) return entity;
        entity.valid = true;
        entity.content = JSON.parseObject(param, Map.class);
        return entity;
    }

    public static Entity of(ResponseEntity<String> resp) {
        Entity entity = new Entity();
        if (resp == null) return entity;
        entity.resp = resp;
        String body = resp.getBody();
        if (body == null) return entity;
        entity.content = JSON.parseObject(body, Map.class);
        if (entity.content == null) return entity;
        Object success = entity.content.get("success");
        if (!(success instanceof Boolean && (Boolean) success)) return entity;
        entity.valid = true;
        return entity;
    }

    @SuppressWarnings("unchecked")
    public Entity put(String key, Object value) {
        if (StringUtils.isEmpty(key)) return this;
        String[] path = key.split(" ");
        if (path.length <= 1) content.put(key, value);
        else {
            Object obj = get(Arrays.copyOf(path, path.length - 1));
            if (obj instanceof Map) ((Map) obj).put(path[path.length - 1], value);
        }
        return this;
    }

    public Entity put(Object obj) {
        if (obj == null) return this;
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Ignore.class)) continue;
                field.setAccessible(true);
                Object val = field.get(obj);
                if (field.getType().isEnum()) val = field.getType().getMethod("getCode").invoke(val);
                if (field.isAnnotationPresent(Level.class)) {
                    put(field.getAnnotation(Level.class).value(), val);
                } else {
                    put(field.getName(), val);
                }
            }
            return this;
        }
        catch (Exception e) {
            log.error("put failed, e = {}", e);
            return this;
        }
    }

    public Entity putNull(String... path) {
        for (String v : path) put(v, null);
        return this;
    }

    public <T> T to(Class<T> clazz) {
        return valid ? to(clazz.isAnnotationPresent(Level.class) ? get(clazz.getAnnotation(Level.class).value()) : content, clazz) : null;
    }

    public <T> List<T> toList(Class<T> clazz) {
        if (!valid) return null;
        Object obj = clazz.isAnnotationPresent(Level.class) ? get(clazz.getAnnotation(Level.class).value()) : content;
        if (obj instanceof List) {
            List<T> list = new ArrayList<>();
            for (Object v : (List) obj) {
                T r = to(v, clazz);
                if (r != null) list.add(r);
            }
            return list.size() == 0 ? null : list;
        }
        return null;
    }

    @SuppressWarnings("all")
    private <T> T to(Object res, Class<T> clazz) {
        if (res == null || clazz == null) return null;
        if (res instanceof List)
            return clazz == List.class ? (T) res : ((List) res).size() == 0 ? null : to(((List) res).get(0), clazz);
        try {
            if (clazz.isEnum())
                return (T) clazz.getMethod("of", String.class).invoke(null, String.valueOf(res));
            if (!(res instanceof Map)) {
                String s = String.valueOf(res);
                if (clazz == String.class) return (T) s;
                if (clazz == Integer.class) return (T) Integer.valueOf(s);
                if (clazz == Boolean.class) return (T) Boolean.valueOf(s);
                if (clazz == Long.class) return (T) Long.valueOf(s);
                if (clazz == Double.class) return (T) Double.valueOf(s);
                if (clazz == Float.class) return (T) Float.valueOf(s);
                if (clazz == Byte.class) return (T) Byte.valueOf(s);
                if (clazz == Short.class) return (T) Short.valueOf(s);
                return JSON.parseObject(s, clazz);
            }
            Map map = (Map) res;
            T obj = clazz.newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Ignore.class)) continue;
                field.setAccessible(true);
                if (field.isAnnotationPresent(Level.class)) {
                    Level level = field.getAnnotation(Level.class);
                    Object val = get(map, level.value());
                    if (field.getType() != List.class) field.set(obj, to(val, field.getType()));
                    else {
                        List value = new ArrayList();
                        if (val instanceof Iterable) for (Object v : (Iterable) val) value.add(to(v, level.type()));
                        field.set(obj, value);
                    }
                } else {
                    Object val = get(map, field.getName());
                    field.set(obj, val == null ? null : to(val, field.getType()));
                }
            }
            return obj;
        } catch (Exception e) {
            log.error("parse failed, e = {}", e);
            return null;
        }
    }

    public Map get() {
        return valid ? content : null;
    }

    public Object get(String... path) {
        return valid ? get(content, path) : null;
    }

    public List<Entity> ofList(String... path) {
        if (valid) return null;
        List<Entity> list = new ArrayList<>();
        Object obj = get(content, path);
        if (obj instanceof List) {
            for (Object v : (List) obj) {
                if (v instanceof Map) {
                    Entity entity = new Entity();
                    entity.valid = true;
                    entity.content = (Map) v;
                    list.add(entity);
                }
            }
        }
        return list;
    }

    private static Object get(Map map, String... path) {
        if (path.length == 1) path = path[0].split(" ");
        Object obj = null;
        for (int i = 0; i < path.length; i++) {
            obj = map.get(path[i]);
            if (obj instanceof List && i < path.length - 1) obj = ((List) obj).get(0);
            if (obj instanceof Map) map = (Map) obj;
            else if (obj == null || i < path.length - 1) return null;
        }
        return obj;
    }

    public boolean isValid() {
        return valid;
    }

    public void asset(String message) {
        if (valid) return;
        log.error("entity invalid, code = {}, body = {}", resp.getStatusCode(), resp.getBody());
        throw new RuntimeException(message);
    }

    @Override
    public String toString() {
        return valid && content != null ? JSON.toJSONString(content) : "";
    }
}
