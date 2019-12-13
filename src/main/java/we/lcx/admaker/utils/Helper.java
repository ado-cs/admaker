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
        if (CollectionUtils.isEmpty(map) || path.length == 0) return null;
        Object obj = null;
        for (int i = 0; i < path.length; i++) {
            obj = map.get(path[i]);
            if (obj instanceof Map) map = (Map) obj;
            else if (obj == null || i < path.length - 1) return null;
        }
        return obj;
    }

    public static String replace(String str, String oldStr, String newStr) {
        int i = str.indexOf(oldStr);
        if (i == -1) return str;
        return str.substring(0, i) + newStr + str.substring(i + oldStr.length());
    }
}
