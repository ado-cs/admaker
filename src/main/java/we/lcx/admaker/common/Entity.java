package we.lcx.admaker.common;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import we.lcx.admaker.common.annotation.Ignore;
import we.lcx.admaker.common.annotation.Level;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

/**
 * Created by LinChenxiao on 2019/12/20 12:03
 **/
@Slf4j
public class Entity {
    private LinkedList<Object> nodes = new LinkedList<>();

    Entity(Map root) {
        this.nodes.add(root);
    }

    public static Entity of(String param) {
        Map map = JSON.parseObject(param, Map.class);
        return new Entity(map == null ? new HashMap() : map);
    }

    @SuppressWarnings("unchecked")
    public Entity cd(String str) {
        if (StringUtils.isEmpty(str)) return this;
        boolean flag = true;
        for (String s : str.split("/")) {
            int i = s.indexOf('[');
            int idx = -1;
            if (i != -1) {
                idx = parseIndex(s);
                if (idx < 0) throw new RuntimeException("Entity:cd 路径不合法");
                s = s.substring(0, i);
            }
            if (s.equals("..")) {
                if (nodes.pollLast() == null || nodes.getLast() == null) throw new RuntimeException("Entity:cd 路径超过边界");
            }
            else if (s.equals("")){
                if (flag) {
                    Object obj = nodes.getFirst();
                    nodes.clear();
                    nodes.add(obj);
                }
            }
            else if (!s.equals(".")) {
                Object obj = nodes.getLast();
                if (obj instanceof Map) {
                    Map map = (Map) obj;
                    if (map.containsKey(s)) nodes.addLast(map.get(s));
                    else {
                        Map newMap = new HashMap();
                        map.put(s, newMap);
                        nodes.addLast(newMap);
                    }
                }
                else throw new RuntimeException("Entity:cd 路径错误，试图访问非Map对象");

            }
            if (i != -1) {
                Object obj = nodes.getLast();
                if (obj instanceof List) {
                    List list = (List) obj;
                    if (idx >= list.size()) throw new RuntimeException("Entity:cd 路径索引超过数组范围");
                    nodes.addLast(list.get(idx));
                }
                else throw new RuntimeException("Entity:cd 路径错误，试图访问非数组对象");
            }
            flag = false;
        }
        return this;
    }
    
    private int parseIndex(String str) {
        if (StringUtils.isEmpty(str)) return -1;
        int i = str.lastIndexOf('[');
        int j = str.lastIndexOf(']');
        if (i == j || i == j - 1) return 0;
        if (i == -1 || j != str.length() - 1) return -1;
        try {
            return Integer.parseInt(str.substring(i + 1, j));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    public Entity newList(String str) {
        Object obj = nodes.getLast();
        if (obj instanceof Map) {
            List list = new ArrayList();
            ((Map) obj).put(str, list);
            nodes.addLast(list);
            return this;
        }
        throw new RuntimeException("Entity:newList 路径错误，试图访问非Map对象");
    }

    public Entity add() {
        if (nodes.pollLast() == null) throw new RuntimeException("Entity:add 当前路径超过边界");
        if (!(nodes.getLast() instanceof List)) throw new RuntimeException("Entity:add 未发现List");
        return this;
    }

    @SuppressWarnings("unchecked")
    public Entity put(String key, Object value) {
        if (StringUtils.isEmpty(key)) return this;
        Object obj = nodes.getLast();
        if (obj == null) throw new RuntimeException("Entity:put 当前路径超过边界");
        if (obj instanceof List) {
            Map map = new HashMap();
            map.put(key, value);
            ((List) obj).add(map);
            nodes.addLast(map);
        }
        else if (obj instanceof Map) ((Map) obj).put(key, value);
        else throw new RuntimeException("Entity:put 当前对象不支持");
        return this;
    }

    public Object get(String key) {
        Object obj = nodes.getLast();
        if (obj == null) throw new RuntimeException("Entity:get 当前路径超过边界");
        if (obj instanceof Map) return get(((Map) obj), key);
        else throw new RuntimeException("Entity:get 当前非map对象");
    }

    public Entity each(Function<Map, Boolean> f) {
        Object obj = nodes.getLast();
        if (obj instanceof List) {
            for (Object v : (List) obj) {
                if (v instanceof Map && f.apply((Map) v)) {
                    nodes.addLast(v);
                    break;
                }
            }
        }
        else throw new RuntimeException("Entity:each 当前非list对象");
        return this;
    }

    public <T> T to(Class<T> clazz) {
        Object obj;
        if (clazz.isAnnotationPresent(Level.class)) obj = get((Map) nodes.getFirst(), clazz.getAnnotation(Level.class).value());
        else obj = nodes.getLast();
        return to(obj, clazz);
    }

    public <T> List<T> toList(Class<T> clazz) {
        Object obj;
        if (clazz.isAnnotationPresent(Level.class)) obj = get((Map) nodes.getFirst(), clazz.getAnnotation(Level.class).value());
        else obj = nodes.getLast();
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
    private static <T> T to(Object res, Class<T> clazz) {
        if (res == null || clazz == null) return null;
        if (res instanceof List) {
            List list = (List) res;
            if (clazz == List.class) return (T) res;
            return list.size() == 0 ? null : to(list.get(0), clazz);
        }
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
                    field.set(obj, to(val, field.getType()));
                }
            }
            return obj;
        } catch (Exception e) {
            log.error("parse failed, e = {}", e);
            return null;
        }
    }

    private static Object get(Map map, String str) {
        String[] path = str.split(" ");
        Object obj = null;
        for (int i = 0; i < path.length; i++) {
            obj = map.get(path[i]);
            if (obj instanceof List && i < path.length - 1) obj = ((List) obj).get(0);
            if (obj instanceof Map) map = (Map) obj;
            else if (obj == null || i < path.length - 1) return null;
        }
        return obj;
    }

    public Map getHead() {
        return (Map) nodes.getFirst();
    }

    public Object getCurrent() {
        return nodes.getLast();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(nodes.getLast());
    }
}
