package we.lcx.admaker.utils;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by LinChenxiao on 2019/12/12 19:29
 **/
public class Helper {

    public static String parseBody(String body, String key) {
        if (StringUtils.isEmpty(body)) return null;
        int i = body.indexOf('"' + key + '"');
        if (i == -1) return null;
        int flag = 0;
        StringBuilder s = new StringBuilder();
        while (++i < body.length()) {
            char c = body.charAt(i);
            if (flag == 0) {
                if (c == ':') flag++;
            }
            else if (flag == 1) {
                if (c != ' ') {
                    if (c == ',' || c == '}') return null;
                    else {
                        s.append(c);
                        flag++;
                    }
                }
            }
            else if (flag == 2) {
                if (c == ',' || c == '}') break;
                s.append(c);
            }
        }
        return s.charAt(0) == '"' ? s.substring(1, s.length() - 1) : s.toString();
    }

    public static String randomSuffix(int length) {
        String s = UUID.randomUUID().toString().replace("-", "");
        return "_" + (s.length() <= length ? s : s.substring(0, length));
    }

    public static boolean valid(Map map, String target, String... path) {
        return target != null && target.equalsIgnoreCase(getString(map, path));
    }

    public static String getString(Map map, String... path) {
        Object obj = getObject(map, path);
        return obj == null ? null : obj.toString();
    }

    public static List getList(Map map, String... path) {
        Object obj = getObject(map, path);
        return obj instanceof List ? (List) obj : null;
    }

    private static Object getObject(Map map, String... path) {
        return get(map, path.length, path);
    }

    public static Map getMap(Map map, String... path) {
        Object obj = get(map, path.length - 1, path);
        return obj instanceof Map ? (Map) obj : null;
    }

    private static Object get(Map map, int depth, String... path) {
        if (CollectionUtils.isEmpty(map) || path.length == 0 || depth < 1 || depth > path.length) return null;
        Object obj = null;
        for (int i = 0; i < depth; i++) {
            obj = map.get(path[i]);
            if (obj instanceof List && i < path.length - 1) obj = ((List) obj).get(0);
            if (obj instanceof Map) map = (Map) obj;
            else if (obj == null || i < path.length - 1) return null;
        }
        return obj;

    }

    public static String repeat(String limit) {
        try {
            int len = Integer.parseInt(limit);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < len; i++) {
                s.append('1');
            }
            return s.toString();
        }
        catch (NumberFormatException e) {
            return "1234567890";
        }
    }

    public static String replace(String str, String oldStr, String newStr) {
        int i = str.indexOf(oldStr);
        if (i == -1) return str;
        return str.substring(0, i) + newStr + str.substring(i + oldStr.length());
    }

    public static String parseDate(String date) {
        return date + "T00:00:00.000Z";
    }
}
