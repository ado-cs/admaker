package we.lcx.admaker.utils;

import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/12 19:29
 **/
public class WordsTool {
    public static List toList(Object obj) {
        List list = new ArrayList();
        list.add(obj);
        return list;
    }

    public static String randomSuffix(int length) {
        String s = UUID.randomUUID().toString().replace("-", "");
        return "_" + (s.length() <= length ? s : s.substring(0, length));
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

    public static String parseDate(String date) {
        return date + "T00:00:00.000Z";
    }
}
