package we.lcx.admaker.utils;

import org.springframework.util.CollectionUtils;
import we.lcx.admaker.common.VisibleException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by LinChenxiao on 2019/12/12 19:29
 **/
public class CommonUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static boolean notSingle(Collection... lists) {
        for (Collection v : lists) {
            if (CollectionUtils.isEmpty(v) || v.size() != 1) return true;
        }
        return false;
    }

    public static boolean contains(Collection list, Object value) {
        return !notContains(list, value);
    }

    public static boolean notContains(Collection list, Object value) {
        return CollectionUtils.isEmpty(list) || !list.contains(value);
    }

    public static String randomSuffix(int length) {
        String s = UUID.randomUUID().toString().replace("-", "");
        return "_" + (s.length() <= length ? s : s.substring(0, length));
    }

    public static String getLimitLength(Integer lowerLength, Integer maxLength) {
        int min = lowerLength == null ? 0 : lowerLength;
        int max = maxLength == null ? 0 : maxLength;
        if (max <= min || min <= 0) return "10";
        return String.valueOf((int) Math.floor((min + max) / 2.0));
    }

    public static String repeat(String limit) {
        try {
            int len = Integer.parseInt(limit);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < len; i++) {
                s.append('1');
            }
            return s.toString();
        } catch (NumberFormatException e) {
            return "1234567890";
        }
    }

    public static String convertName(String str) {
        if (str == null || str.length() < 2) return str;
        str = str.replace(" ", "");
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static String[] splitName(String str, String splitStr, int length, int ignoreTail) {
        String[] s = str.split(splitStr);
        if (s.length < length + ignoreTail) return s;
        String[] ns = new String[length];
        for (int i = length - 1, j = s.length - ignoreTail - 1; j >= 0; i--, j--) {
            if (i < 0) ns[0] = s[j] + ns[0];
            else ns[i] = s[j];
        }
        return ns;
    }

    public static String parseDateString(String date) {
        return date + "T00:00:00.000Z";
    }

    private static Date parseDate(String date) {
        try {
            return FORMAT.parse(date);
        } catch (Exception e) {
            throw new VisibleException("时间文本格式错误");
        }
    }

    public static long timeOfToday() {
        Calendar calendar = Calendar.getInstance();
        return parseTime(calendar.get(Calendar.YEAR)  + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DATE));
    }

    public static long parseTime(String date) {
        return parseDate(date).getTime();
    }
}
