package com.kiona.analysis.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author yangshuaichao
 * @date 2022/08/02 18:54
 * @description TODO
 */
public class DateFormatter {
    private static final List<String> DEFAULT_PATTERNS = Arrays.asList("y-M-d", "y/M/d");

    private static final HashMap<String, SimpleDateFormat> SIMPLE_DATE_FORMAT_CACHE = new HashMap<>();
    private static final HashMap<String, DateTimeFormatter> DATETIME_FORMAT_CACHE = new HashMap<>();

    private static final Map<String, Date> DATE_CACHE = new HashMap<>();
    private static final Map<String, LocalDate> LOCAL_DATE_CACHE = new HashMap<>();
    private static final Map<String, String> DATE_STRING_CACHE = new HashMap<>();

    public static Date parse(String dateString, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("日期解析模型不能为空");
        }
        for (String pattern : patterns) {
            try {
                return parse(dateString, pattern);
            } catch (ParseException ignored) {}
        }
        throw new IllegalArgumentException("日期字符串不可解析：" + dateString);
    }

    public static Date parse(String dateString, String pattern) throws ParseException {
        if (DATE_CACHE.containsKey(dateString)) {
            return DATE_CACHE.get(dateString);
        }
        Date parse = getSimpleDateFormat(pattern).parse(dateString);
        DATE_CACHE.put(dateString, parse);
        return parse;
    }


    public static Date tryParse(String dateString) {
        return parse(dateString, DEFAULT_PATTERNS);
    }


    public static LocalDate parseLocalDate(String dateString, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("日期解析模型不能为空");
        }
        for (String pattern : patterns) {
            try {
                return parseLocalDate(dateString, pattern);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("日期字符串不可解析：" + dateString);
    }

    public static LocalDate parseLocalDate(String dateString, String pattern) {
        if (LOCAL_DATE_CACHE.containsKey(dateString)) {
            return LOCAL_DATE_CACHE.get(dateString);
        }
        LocalDate parse = LocalDate.parse(dateString, getDateTimeFormatter(pattern));
        LOCAL_DATE_CACHE.put(dateString, parse);
        return parse;
    }

    public static LocalDate tryParseLocalDate(String dateString) {
        return parseLocalDate(dateString, DEFAULT_PATTERNS);
    }


    public static String format(Date date, String pattern) {
        String key = date.toString() + "|" + pattern;
        if (DATE_STRING_CACHE.containsKey(key)) {
            return DATE_STRING_CACHE.get(key);
        }
        String format = getSimpleDateFormat(pattern).format(date);
        DATE_STRING_CACHE.put(key, format);
        return format;
    }

    public static String format(Date date) {
        return getSimpleDateFormat("yyyy-MM-dd").format(date);
    }


    private static SimpleDateFormat getSimpleDateFormat(String pattern) {
        if (SIMPLE_DATE_FORMAT_CACHE.containsKey(pattern)) {
            return SIMPLE_DATE_FORMAT_CACHE.get(pattern);
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            SIMPLE_DATE_FORMAT_CACHE.put(pattern, simpleDateFormat);
            return simpleDateFormat;
        }
    }

    private static DateTimeFormatter getDateTimeFormatter(String pattern) {
        if (DATETIME_FORMAT_CACHE.containsKey(pattern)) {
            return DATETIME_FORMAT_CACHE.get(pattern);
        } else {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            DATETIME_FORMAT_CACHE.put(pattern, dateTimeFormatter);
            return dateTimeFormatter;
        }
    }

    public static void main(String[] args) {
        String dateString1 = "2022/7/11";
        String dateString2 = "2022/7/3";
        String dateString3 = "2022-7-11";
        String dateString4 = "2022-7-3";
        String dateString5 = "2022/07/03";
        String dateString6 = "2022-07-03";
        System.out.println(tryParse(dateString1));
        System.out.println(tryParse(dateString2));
        System.out.println(tryParse(dateString3));
        System.out.println(tryParse(dateString4));
        System.out.println(tryParse(dateString5));
        System.out.println(tryParse(dateString6));
    }
}
