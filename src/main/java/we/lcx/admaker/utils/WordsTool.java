package we.lcx.admaker.utils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/12 19:29
 **/
public class WordsTool {
    private static final SimpleDateFormat FORMAT1 = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat FORMAT2 = new SimpleDateFormat("yyyy-MM-dd");

    public static List toList(Object... obj) {
        return Arrays.asList(obj);
    }

    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomSuffix(int length) {
        String s = generateId();
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

    public static String convertName(String str) {
        if (str == null || str.length() < 2) return str;
        str = str.replace(" ", "");
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static String getNowDate() {
        return FORMAT1.format(new Date());
    }

    public static String parseDateString(String date) {
        return date + "T00:00:00.000Z";
    }

    public static Date parseDate(String date) {
        try {
            return FORMAT2.parse(date);
        }
        catch (Exception e) {
            throw new RuntimeException("时间文本格式错误");
        }
    }

    public static long parseTime(String date) {
        return parseDate(date).getTime();
    }
}
