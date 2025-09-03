package com.vscodelife.socketio.util;

/**
 * 日期工具類
 * 提供各種日期比較和格式化功能
 * 支援 timestamp、java.util.Date 和 joda-time 類型
 */
public final class DateUtil {

    // 私有建構函數，防止實例化
    private DateUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // 常用日期格式
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    // ========== 解析普通時間字串方法 ==========

    /**
     * 解析普通時間格式字串獲得時間戳
     * 支援格式：yyyy-MM-dd HH:mm:ss, yyyy-MM-dd, yyyy-MM-dd HH:mm:ss.SSS
     */
    public static long parseToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return 0;
        }
        String trimmedStr = dateStr.trim();

        try {
            // 嘗試 yyyy-MM-dd HH:mm:ss.SSS 格式
            if (trimmedStr.length() >= 23 && trimmedStr.contains(".")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_SSS);
                return org.joda.time.DateTime.parse(trimmedStr, formatter).getMillis();
            }
            // 嘗試 yyyy-MM-dd HH:mm:ss 格式
            else if (trimmedStr.length() >= 19 && trimmedStr.contains(" ") && trimmedStr.contains(":")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
                return org.joda.time.DateTime.parse(trimmedStr, formatter).getMillis();
            }
            // 嘗試 yyyy-MM-dd 格式
            else if (trimmedStr.length() >= 10 && trimmedStr.contains("-")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD);
                return org.joda.time.LocalDate.parse(trimmedStr, formatter).toDateTimeAtStartOfDay().getMillis();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 解析普通時間格式字串獲得 Date 物件
     * 支援格式：yyyy-MM-dd HH:mm:ss, yyyy-MM-dd, yyyy-MM-dd HH:mm:ss.SSS
     */
    public static java.util.Date parseToDate(String dateStr) {
        long timestamp = parseToTimestamp(dateStr);
        return timestamp > 0 ? new java.util.Date(timestamp) : null;
    }

    /**
     * 解析普通時間格式字串獲得 LocalDate 物件
     * 支援格式：yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm:ss.SSS
     */
    public static org.joda.time.LocalDate parseToLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmedStr = dateStr.trim();

        try {
            // 如果包含時間部分，先解析為 DateTime 再轉換
            if (trimmedStr.contains(" ") && trimmedStr.contains(":")) {
                org.joda.time.DateTime dateTime = parseToDateTime(trimmedStr);
                return dateTime != null ? dateTime.toLocalDate() : null;
            }
            // 直接解析日期部分
            else if (trimmedStr.length() >= 10 && trimmedStr.contains("-")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD);
                return org.joda.time.LocalDate.parse(trimmedStr, formatter);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析普通時間格式字串獲得 DateTime 物件
     * 支援格式：yyyy-MM-dd HH:mm:ss, yyyy-MM-dd, yyyy-MM-dd HH:mm:ss.SSS
     */
    public static org.joda.time.DateTime parseToDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmedStr = dateStr.trim();

        try {
            // 嘗試 yyyy-MM-dd HH:mm:ss.SSS 格式
            if (trimmedStr.length() >= 23 && trimmedStr.contains(".")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_SSS);
                return org.joda.time.DateTime.parse(trimmedStr, formatter);
            }
            // 嘗試 yyyy-MM-dd HH:mm:ss 格式
            else if (trimmedStr.length() >= 19 && trimmedStr.contains(" ") && trimmedStr.contains(":")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
                return org.joda.time.DateTime.parse(trimmedStr, formatter);
            }
            // 嘗試 yyyy-MM-dd 格式
            else if (trimmedStr.length() >= 10 && trimmedStr.contains("-")) {
                org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                        .forPattern(DATE_FORMAT_YYYY_MM_DD);
                return org.joda.time.LocalDate.parse(trimmedStr, formatter).toDateTimeAtStartOfDay();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== 時間轉字串方法 ==========

    /**
     * 將時間戳轉換為標準時間字串
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    public static String toTimeStr(long timestamp) {
        return formatTimestamp(timestamp, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 將 Date 物件轉換為標準時間字串
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    public static String toTimeStr(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return formatDate(date, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 將 LocalDate 物件轉換為標準日期字串
     * 格式：yyyy-MM-dd (時間部分為 00:00:00)
     */
    public static String toTimeStr(org.joda.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toDateTimeAtStartOfDay().toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 將 DateTime 物件轉換為標準時間字串
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    public static String toTimeStr(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
    }

    // ========== 解析 ISO8601 時間字串方法 ==========

    /**
     * 解析 ISO8601 格式字串獲得時間戳
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static long parseIso8601ToTimestamp(String iso8601Str) {
        if (iso8601Str == null || iso8601Str.trim().isEmpty()) {
            return 0;
        }
        try {
            org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                    .forPattern(DATE_FORMAT_ISO8601);
            org.joda.time.DateTime dateTime = org.joda.time.DateTime.parse(iso8601Str, formatter);
            return dateTime.getMillis();
        } catch (Exception e) {
            // 如果解析失敗，嘗試使用標準 ISO8601 解析器
            try {
                org.joda.time.DateTime dateTime = org.joda.time.DateTime.parse(iso8601Str);
                return dateTime.getMillis();
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    /**
     * 解析 ISO8601 格式字串獲得 Date 物件
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static java.util.Date parseIso8601ToDate(String iso8601Str) {
        long timestamp = parseIso8601ToTimestamp(iso8601Str);
        return timestamp > 0 ? new java.util.Date(timestamp) : null;
    }

    /**
     * 解析 ISO8601 格式字串獲得 LocalDate 物件
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     * 注意：只提取日期部分，忽略時間部分
     */
    public static org.joda.time.LocalDate parseIso8601ToLocalDate(String iso8601Str) {
        if (iso8601Str == null || iso8601Str.trim().isEmpty()) {
            return null;
        }
        try {
            // 先解析為 DateTime，然後轉換為 LocalDate
            org.joda.time.DateTime dateTime = parseIso8601ToDateTime(iso8601Str);
            return dateTime != null ? dateTime.toLocalDate() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 ISO8601 格式字串獲得 DateTime 物件
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static org.joda.time.DateTime parseIso8601ToDateTime(String iso8601Str) {
        if (iso8601Str == null || iso8601Str.trim().isEmpty()) {
            return null;
        }
        try {
            org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                    .forPattern(DATE_FORMAT_ISO8601);
            return org.joda.time.DateTime.parse(iso8601Str, formatter);
        } catch (Exception e) {
            // 如果解析失敗，嘗試使用標準 ISO8601 解析器
            try {
                return org.joda.time.DateTime.parse(iso8601Str);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // ========== ISO8601 時間格式方法 ==========

    /**
     * 取得 ISO8601 格式時間字串（時間戳）
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static String toIso8601(long timestamp) {
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                .forPattern(DATE_FORMAT_ISO8601);
        return new org.joda.time.DateTime(timestamp).toString(formatter);
    }

    /**
     * 取得 ISO8601 格式時間字串（Date）
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static String toIso8601(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return toIso8601(date.getTime());
    }

    /**
     * 取得 ISO8601 格式日期字串（LocalDate）
     * 格式：yyyy-MM-dd
     */
    public static String toIso8601(org.joda.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toString("yyyy-MM-dd");
    }

    /**
     * 取得 ISO8601 格式時間字串（DateTime）
     * 格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static String toIso8601(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat
                .forPattern(DATE_FORMAT_ISO8601);
        return dateTime.toString(formatter);
    }

    // ========== 判斷是否同年 ==========

    /**
     * 判斷兩個時間戳是否在同一年
     */
    public static boolean isSameYear(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return date1.getYear() == date2.getYear();
    }

    /**
     * 判斷兩個 Date 物件是否在同一年
     */
    public static boolean isSameYear(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return isSameYear(date1.getTime(), date2.getTime());
    }

    /**
     * 判斷兩個 LocalDate 是否在同一年
     */
    public static boolean isSameYear(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.getYear() == date2.getYear();
    }

    /**
     * 判斷兩個 DateTime 是否在同一年
     */
    public static boolean isSameYear(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.getYear() == dateTime2.getYear();
    }

    // ========== 判斷是否同月 ==========

    /**
     * 判斷兩個時間戳是否在同一月
     */
    public static boolean isSameMonth(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return date1.getYear() == date2.getYear() &&
                date1.getMonthOfYear() == date2.getMonthOfYear();
    }

    /**
     * 判斷兩個 Date 物件是否在同一月
     */
    public static boolean isSameMonth(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return isSameMonth(date1.getTime(), date2.getTime());
    }

    /**
     * 判斷兩個 LocalDate 是否在同一月
     */
    public static boolean isSameMonth(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.getYear() == date2.getYear() &&
                date1.getMonthOfYear() == date2.getMonthOfYear();
    }

    /**
     * 判斷兩個 DateTime 是否在同一月
     */
    public static boolean isSameMonth(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.getYear() == dateTime2.getYear() &&
                dateTime1.getMonthOfYear() == dateTime2.getMonthOfYear();
    }

    // ========== 判斷是否同日 ==========

    /**
     * 判斷兩個時間戳是否在同一天
     */
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    /**
     * 判斷兩個 Date 物件是否在同一天
     */
    public static boolean isSameDay(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return isSameDay(date1.getTime(), date2.getTime());
    }

    /**
     * 判斷兩個 LocalDate 是否在同一天
     */
    public static boolean isSameDay(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.equals(date2);
    }

    /**
     * 判斷兩個 DateTime 是否在同一天
     */
    public static boolean isSameDay(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }

    /**
     * 判斷是否為今天
     */
    public static boolean isToday(long timestamp) {
        return isSameDay(timestamp, getCurrentTimestamp());
    }

    /**
     * 判斷是否為今天
     */
    public static boolean isToday(java.util.Date date) {
        return date != null && isToday(date.getTime());
    }

    /**
     * 判斷是否為今天
     */
    public static boolean isToday(org.joda.time.LocalDate date) {
        return date != null && date.equals(getCurrentDate());
    }

    /**
     * 判斷是否為今天（DateTime）
     */
    public static boolean isToday(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(getCurrentDate());
    }

    // ========== 判斷是否在日期範圍內 ==========

    /**
     * 判斷 now 時間戳是否在 timestamp1 和 timestamp2 之間（包含邊界）
     */
    public static boolean isBetweenDate(long timestamp1, long timestamp2, long now) {
        long start = Math.min(timestamp1, timestamp2);
        long end = Math.max(timestamp1, timestamp2);
        return now >= start && now <= end;
    }

    /**
     * 判斷 now 是否在 date1 和 date2 之間（包含邊界）
     */
    public static boolean isBetweenDate(java.util.Date date1, java.util.Date date2, java.util.Date now) {
        if (date1 == null || date2 == null || now == null) {
            return false;
        }
        return isBetweenDate(date1.getTime(), date2.getTime(), now.getTime());
    }

    /**
     * 判斷 now 是否在 date1 和 date2 之間（包含邊界）
     */
    public static boolean isBetweenDate(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2,
            org.joda.time.LocalDate now) {
        if (date1 == null || date2 == null || now == null) {
            return false;
        }
        org.joda.time.LocalDate start = date1.isBefore(date2) ? date1 : date2;
        org.joda.time.LocalDate end = date1.isBefore(date2) ? date2 : date1;
        return (now.equals(start) || now.isAfter(start)) &&
                (now.equals(end) || now.isBefore(end));
    }

    /**
     * 判斷 now 是否在 dateTime1 和 dateTime2 之間（包含邊界）
     */
    public static boolean isBetweenDate(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2,
            org.joda.time.DateTime now) {
        if (dateTime1 == null || dateTime2 == null || now == null) {
            return false;
        }
        org.joda.time.DateTime start = dateTime1.isBefore(dateTime2) ? dateTime1 : dateTime2;
        org.joda.time.DateTime end = dateTime1.isBefore(dateTime2) ? dateTime2 : dateTime1;
        return (now.equals(start) || now.isAfter(start)) &&
                (now.equals(end) || now.isBefore(end));
    }

    // ========== 計算時間差（年） ==========

    /**
     * 計算兩個時間戳之間的年份差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenYears(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Years.yearsBetween(date2, date1).getYears();
    }

    /**
     * 計算兩個 Date 之間的年份差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenYears(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenYears(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 LocalDate 之間的年份差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenYears(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return org.joda.time.Years.yearsBetween(date2, date1).getYears();
    }

    /**
     * 計算兩個 DateTime 之間的年份差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenYears(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Years.yearsBetween(dateTime2, dateTime1).getYears();
    }

    // ========== 計算時間差（月） ==========

    /**
     * 計算兩個時間戳之間的月份差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenMonths(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Months.monthsBetween(date2, date1).getMonths();
    }

    /**
     * 計算兩個 Date 之間的月份差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenMonths(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenMonths(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 LocalDate 之間的月份差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenMonths(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return org.joda.time.Months.monthsBetween(date2, date1).getMonths();
    }

    /**
     * 計算兩個 DateTime 之間的月份差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenMonths(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Months.monthsBetween(dateTime2, dateTime1).getMonths();
    }

    // ========== 計算時間差（週） ==========

    /**
     * 計算兩個時間戳之間的週數差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenWeeks(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Weeks.weeksBetween(date2, date1).getWeeks();
    }

    /**
     * 計算兩個 Date 之間的週數差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenWeeks(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenWeeks(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 LocalDate 之間的週數差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenWeeks(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return org.joda.time.Weeks.weeksBetween(date2, date1).getWeeks();
    }

    /**
     * 計算兩個 DateTime 之間的週數差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenWeeks(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Weeks.weeksBetween(dateTime2, dateTime1).getWeeks();
    }

    // ========== 計算時間差（天） ==========

    /**
     * 計算兩個時間戳之間的天數差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenDays(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Days.daysBetween(date2, date1).getDays();
    }

    /**
     * 計算兩個 Date 之間的天數差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenDays(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenDays(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 LocalDate 之間的天數差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenDays(org.joda.time.LocalDate date1, org.joda.time.LocalDate date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return org.joda.time.Days.daysBetween(date2, date1).getDays();
    }

    /**
     * 計算兩個 DateTime 之間的天數差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenDays(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Days.daysBetween(dateTime2, dateTime1).getDays();
    }

    // ========== 計算時間差（小時） ==========

    /**
     * 計算兩個時間戳之間的小時差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenHours(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Hours.hoursBetween(date2, date1).getHours();
    }

    /**
     * 計算兩個 Date 之間的小時差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenHours(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenHours(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 DateTime 之間的小時差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenHours(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Hours.hoursBetween(dateTime2, dateTime1).getHours();
    }

    // ========== 計算時間差（分鐘） ==========

    /**
     * 計算兩個時間戳之間的分鐘差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenMinutes(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Minutes.minutesBetween(date2, date1).getMinutes();
    }

    /**
     * 計算兩個 Date 之間的分鐘差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenMinutes(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenMinutes(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 DateTime 之間的分鐘差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenMinutes(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Minutes.minutesBetween(dateTime2, dateTime1).getMinutes();
    }

    // ========== 計算時間差（秒） ==========

    /**
     * 計算兩個時間戳之間的秒數差
     * 返回結果 > 0 表示 timestamp1 > timestamp2
     * 返回結果 < 0 表示 timestamp1 < timestamp2
     */
    public static int betweenSeconds(long timestamp1, long timestamp2) {
        org.joda.time.DateTime date1 = new org.joda.time.DateTime(timestamp1);
        org.joda.time.DateTime date2 = new org.joda.time.DateTime(timestamp2);
        return org.joda.time.Seconds.secondsBetween(date2, date1).getSeconds();
    }

    /**
     * 計算兩個 Date 之間的秒數差
     * 返回結果 > 0 表示 date1 > date2
     * 返回結果 < 0 表示 date1 < date2
     */
    public static int betweenSeconds(java.util.Date date1, java.util.Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return betweenSeconds(date1.getTime(), date2.getTime());
    }

    /**
     * 計算兩個 DateTime 之間的秒數差
     * 返回結果 > 0 表示 dateTime1 > dateTime2
     * 返回結果 < 0 表示 dateTime1 < dateTime2
     */
    public static int betweenSeconds(org.joda.time.DateTime dateTime1, org.joda.time.DateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return 0;
        }
        return org.joda.time.Seconds.secondsBetween(dateTime2, dateTime1).getSeconds();
    }

    // ========== 日期加減運算 ==========

    /**
     * 時間戳加年份
     */
    public static long plusYear(long timestamp, int years) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusYears(years).getMillis();
    }

    /**
     * Date 加年份
     */
    public static java.util.Date plusYear(java.util.Date date, int years) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusYear(date.getTime(), years));
    }

    /**
     * LocalDate 加年份
     */
    public static org.joda.time.LocalDate plusYear(org.joda.time.LocalDate date, int years) {
        if (date == null) {
            return null;
        }
        return date.plusYears(years);
    }

    /**
     * DateTime 加年份
     */
    public static org.joda.time.DateTime plusYear(org.joda.time.DateTime dateTime, int years) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusYears(years);
    }

    /**
     * 時間戳加月份
     */
    public static long plusMonth(long timestamp, int months) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusMonths(months).getMillis();
    }

    /**
     * Date 加月份
     */
    public static java.util.Date plusMonth(java.util.Date date, int months) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusMonth(date.getTime(), months));
    }

    /**
     * LocalDate 加月份
     */
    public static org.joda.time.LocalDate plusMonth(org.joda.time.LocalDate date, int months) {
        if (date == null) {
            return null;
        }
        return date.plusMonths(months);
    }

    /**
     * DateTime 加月份
     */
    public static org.joda.time.DateTime plusMonth(org.joda.time.DateTime dateTime, int months) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMonths(months);
    }

    /**
     * 時間戳加天數
     */
    public static long plusDay(long timestamp, int days) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusDays(days).getMillis();
    }

    /**
     * Date 加天數
     */
    public static java.util.Date plusDay(java.util.Date date, int days) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusDay(date.getTime(), days));
    }

    /**
     * LocalDate 加天數
     */
    public static org.joda.time.LocalDate plusDay(org.joda.time.LocalDate date, int days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }

    /**
     * DateTime 加天數
     */
    public static org.joda.time.DateTime plusDay(org.joda.time.DateTime dateTime, int days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * 時間戳加週數
     */
    public static long plusWeek(long timestamp, int weeks) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusWeeks(weeks).getMillis();
    }

    /**
     * Date 加週數
     */
    public static java.util.Date plusWeek(java.util.Date date, int weeks) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusWeek(date.getTime(), weeks));
    }

    /**
     * LocalDate 加週數
     */
    public static org.joda.time.LocalDate plusWeek(org.joda.time.LocalDate date, int weeks) {
        if (date == null) {
            return null;
        }
        return date.plusWeeks(weeks);
    }

    /**
     * DateTime 加週數
     */
    public static org.joda.time.DateTime plusWeek(org.joda.time.DateTime dateTime, int weeks) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusWeeks(weeks);
    }

    /**
     * 時間戳加小時
     */
    public static long plusHour(long timestamp, int hours) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusHours(hours).getMillis();
    }

    /**
     * Date 加小時
     */
    public static java.util.Date plusHour(java.util.Date date, int hours) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusHour(date.getTime(), hours));
    }

    /**
     * DateTime 加小時（注意：LocalDate 不支援時間操作，改為 DateTime）
     */
    public static org.joda.time.DateTime plusHour(org.joda.time.DateTime dateTime, int hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 時間戳加分鐘
     */
    public static long plusMinute(long timestamp, int minutes) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusMinutes(minutes).getMillis();
    }

    /**
     * Date 加分鐘
     */
    public static java.util.Date plusMinute(java.util.Date date, int minutes) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusMinute(date.getTime(), minutes));
    }

    /**
     * DateTime 加分鐘（注意：LocalDate 不支援時間操作，改為 DateTime）
     */
    public static org.joda.time.DateTime plusMinute(org.joda.time.DateTime dateTime, int minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 時間戳加秒數
     */
    public static long plusSecond(long timestamp, int seconds) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.plusSeconds(seconds).getMillis();
    }

    /**
     * Date 加秒數
     */
    public static java.util.Date plusSecond(java.util.Date date, int seconds) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(plusSecond(date.getTime(), seconds));
    }

    /**
     * DateTime 加秒數（注意：LocalDate 不支援時間操作，改為 DateTime）
     */
    public static org.joda.time.DateTime plusSecond(org.joda.time.DateTime dateTime, int seconds) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusSeconds(seconds);
    }

    // ========== 日期信息獲取 ==========

    /**
     * 獲取時間戳在一年中的第幾天
     */
    public static int dayOfYear(long timestamp) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.getDayOfYear();
    }

    /**
     * 獲取 Date 在一年中的第幾天
     */
    public static int dayOfYear(java.util.Date date) {
        if (date == null) {
            return 0;
        }
        return dayOfYear(date.getTime());
    }

    /**
     * 獲取 LocalDate 在一年中的第幾天
     */
    public static int dayOfYear(org.joda.time.LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.getDayOfYear();
    }

    /**
     * 獲取 DateTime 在一年中的第幾天
     */
    public static int dayOfYear(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.getDayOfYear();
    }

    /**
     * 獲取時間戳在一月中的第幾天
     */
    public static int dayOfMonth(long timestamp) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.getDayOfMonth();
    }

    /**
     * 獲取 Date 在一月中的第幾天
     */
    public static int dayOfMonth(java.util.Date date) {
        if (date == null) {
            return 0;
        }
        return dayOfMonth(date.getTime());
    }

    /**
     * 獲取 LocalDate 在一月中的第幾天
     */
    public static int dayOfMonth(org.joda.time.LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.getDayOfMonth();
    }

    /**
     * 獲取 DateTime 在一月中的第幾天
     */
    public static int dayOfMonth(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.getDayOfMonth();
    }

    /**
     * 獲取時間戳是一週中的第幾天（1=週一, 7=週日）
     */
    public static int dayOfWeek(long timestamp) {
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime(timestamp);
        return dateTime.getDayOfWeek();
    }

    /**
     * 獲取 Date 是一週中的第幾天（1=週一, 7=週日）
     */
    public static int dayOfWeek(java.util.Date date) {
        if (date == null) {
            return 0;
        }
        return dayOfWeek(date.getTime());
    }

    /**
     * 獲取 LocalDate 是一週中的第幾天（1=週一, 7=週日）
     */
    public static int dayOfWeek(org.joda.time.LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.getDayOfWeek();
    }

    /**
     * 獲取 DateTime 是一週中的第幾天（1=週一, 7=週日）
     */
    public static int dayOfWeek(org.joda.time.DateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.getDayOfWeek();
    }

    // ========== 額外的工具方法 ==========

    /**
     * 格式化時間戳為指定格式的字串
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = DATE_FORMAT_YYYY_MM_DD_HH_MM_SS;
        }
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern);
        return new org.joda.time.DateTime(timestamp).toString(formatter);
    }

    /**
     * 格式化 Date 為指定格式的字串
     */
    public static String formatDate(java.util.Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return formatTimestamp(date.getTime(), pattern);
    }

    /**
     * 格式化 LocalDate 為指定格式的字串
     */
    public static String formatLocalDate(org.joda.time.LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        if (pattern == null || pattern.isEmpty()) {
            pattern = DATE_FORMAT_YYYY_MM_DD;
        }
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern);
        return date.toString(formatter);
    }

    /**
     * 解析字串為 DateTime
     */
    public static org.joda.time.DateTime parseDateTime(String dateString, String pattern) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        if (pattern == null || pattern.isEmpty()) {
            pattern = DATE_FORMAT_YYYY_MM_DD_HH_MM_SS;
        }
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern);
        return org.joda.time.DateTime.parse(dateString, formatter);
    }

    /**
     * 解析字串為 LocalDate
     */
    public static org.joda.time.LocalDate parseLocalDate(String dateString, String pattern) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        if (pattern == null || pattern.isEmpty()) {
            pattern = DATE_FORMAT_YYYY_MM_DD;
        }
        org.joda.time.format.DateTimeFormatter formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern);
        return org.joda.time.LocalDate.parse(dateString, formatter);
    }

    /**
     * 獲取當前時間戳
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 獲取當前日期（LocalDate）
     */
    public static org.joda.time.LocalDate getCurrentDate() {
        return org.joda.time.LocalDate.now();
    }

    /**
     * 獲取當前日期時間（DateTime）
     */
    public static org.joda.time.DateTime getCurrentDateTime() {
        return org.joda.time.DateTime.now();
    }

    public static void main(String[] args) {
        // 測試日期工具類
        org.joda.time.LocalDate date1 = new org.joda.time.LocalDate(2023, 1, 1);
        org.joda.time.LocalDate date2 = new org.joda.time.LocalDate(2023, 12, 31);
        org.joda.time.LocalDate now = org.joda.time.LocalDate.now();

        System.out.println("=== 基本功能測試 ===");
        System.out.println("isBetweenDate: " + isBetweenDate(date1, date2, now));
        System.out.println("formatLocalDate: " + formatLocalDate(now, DATE_FORMAT_YYYY_MM_DD));
        System.out.println("parseLocalDate: " + parseLocalDate("2023-01-01", DATE_FORMAT_YYYY_MM_DD));
        System.out.println("betweenDays: " + betweenDays(date1, date2));
        System.out.println("isToday: " + isToday(now));

        // 測試 isToday DateTime 版本
        org.joda.time.DateTime todayDateTime = getCurrentDateTime();
        org.joda.time.DateTime yesterday = todayDateTime.minusDays(1);
        org.joda.time.DateTime tomorrow = todayDateTime.plusDays(1);
        System.out.println("isToday(當前DateTime): " + isToday(todayDateTime));
        System.out.println("isToday(昨天DateTime): " + isToday(yesterday));
        System.out.println("isToday(明天DateTime): " + isToday(tomorrow));
        System.out.println("isToday(null DateTime): " + isToday((org.joda.time.DateTime) null));

        System.out.println("\n=== 日期加減測試 ===");
        long currentTimestamp = getCurrentTimestamp();

        // 測試加年份
        System.out.println("當前時間: " + formatTimestamp(currentTimestamp, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加1年: " + formatTimestamp(plusYear(currentTimestamp, 1), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("減1年: " + formatTimestamp(plusYear(currentTimestamp, -1), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試加月份
        System.out.println("加3個月: " + formatTimestamp(plusMonth(currentTimestamp, 3), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out
                .println("減3個月: " + formatTimestamp(plusMonth(currentTimestamp, -3), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試加天數
        System.out.println("加7天: " + formatTimestamp(plusDay(currentTimestamp, 7), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("減7天: " + formatTimestamp(plusDay(currentTimestamp, -7), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試加週數
        System.out.println("加2週: " + formatTimestamp(plusWeek(currentTimestamp, 2), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試加小時
        System.out.println("加5小時: " + formatTimestamp(plusHour(currentTimestamp, 5), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試加分鐘
        System.out.println(
                "加30分鐘: " + formatTimestamp(plusMinute(currentTimestamp, 30), DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試 LocalDate 操作
        System.out.println("\n=== LocalDate 測試 ===");
        System.out.println("當前日期: " + formatLocalDate(now, DATE_FORMAT_YYYY_MM_DD));
        System.out.println("加1年: " + formatLocalDate(plusYear(now, 1), DATE_FORMAT_YYYY_MM_DD));
        System.out.println("加6個月: " + formatLocalDate(plusMonth(now, 6), DATE_FORMAT_YYYY_MM_DD));
        System.out.println("加10天: " + formatLocalDate(plusDay(now, 10), DATE_FORMAT_YYYY_MM_DD));

        // 測試 DateTime 操作
        System.out.println("\n=== DateTime 測試 ===");
        org.joda.time.DateTime currentDateTime = getCurrentDateTime();
        System.out.println("當前時間: " + currentDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加1年: " + plusYear(currentDateTime, 1).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加3個月: " + plusMonth(currentDateTime, 3).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加15天: " + plusDay(currentDateTime, 15).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加2週: " + plusWeek(currentDateTime, 2).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加8小時: " + plusHour(currentDateTime, 8).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加45分鐘: " + plusMinute(currentDateTime, 45).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("加30秒: " + plusSecond(currentDateTime, 30).toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 測試 DateTime 範圍判斷
        System.out.println("\n=== DateTime 範圍測試 ===");
        org.joda.time.DateTime startDateTime = currentDateTime.minusHours(5);
        org.joda.time.DateTime endDateTime = currentDateTime.plusHours(5);
        org.joda.time.DateTime testDateTime = currentDateTime.plusHours(2);

        System.out.println("開始時間: " + startDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("結束時間: " + endDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("測試時間: " + testDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("是否在範圍內: " + isBetweenDate(startDateTime, endDateTime, testDateTime));

        // 測試 DateTime 同年月日判斷
        System.out.println("\n=== DateTime 同年月日測試 ===");
        org.joda.time.DateTime dateTime1 = new org.joda.time.DateTime(2025, 8, 14, 10, 30, 0);
        org.joda.time.DateTime dateTime2 = new org.joda.time.DateTime(2025, 8, 14, 15, 45, 30);
        org.joda.time.DateTime dateTime3 = new org.joda.time.DateTime(2025, 9, 14, 10, 30, 0);
        org.joda.time.DateTime dateTime4 = new org.joda.time.DateTime(2024, 8, 14, 10, 30, 0);

        System.out.println("時間1: " + dateTime1.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("時間2: " + dateTime2.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("時間3: " + dateTime3.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("時間4: " + dateTime4.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        System.out.println("時間1和時間2是否同年: " + isSameYear(dateTime1, dateTime2));
        System.out.println("時間1和時間4是否同年: " + isSameYear(dateTime1, dateTime4));
        System.out.println("時間1和時間2是否同月: " + isSameMonth(dateTime1, dateTime2));
        System.out.println("時間1和時間3是否同月: " + isSameMonth(dateTime1, dateTime3));
        System.out.println("時間1和時間2是否同日: " + isSameDay(dateTime1, dateTime2));
        System.out.println("時間1和時間3是否同日: " + isSameDay(dateTime1, dateTime3));

        // 測試日期信息獲取
        System.out.println("\n=== 日期信息獲取測試 ===");
        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45);
        System.out.println("測試日期: " + testDate.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("一年中的第幾天: " + dayOfYear(testDate));
        System.out.println("一月中的第幾天: " + dayOfMonth(testDate));
        System.out.println("一週中的第幾天: " + dayOfWeek(testDate) + " (1=週一, 7=週日)");

        // 測試當前時間的信息
        System.out.println("\n當前時間信息:");
        long nowTimestamp = getCurrentTimestamp();
        System.out.println("當前時間戳: " + nowTimestamp);
        System.out.println("一年中的第幾天: " + dayOfYear(nowTimestamp));
        System.out.println("一月中的第幾天: " + dayOfMonth(nowTimestamp));
        System.out.println("一週中的第幾天: " + dayOfWeek(nowTimestamp));

        // 測試 ISO8601 格式方法
        System.out.println("\n========== ISO8601 時間格式測試 ==========");

        // 測試 long timestamp
        long timestamp = System.currentTimeMillis();
        System.out.println("原始時間戳: " + timestamp);
        System.out.println("ISO8601格式(long): " + toIso8601(timestamp));

        // 測試 Date
        java.util.Date date = new java.util.Date();
        System.out.println("ISO8601格式(Date): " + toIso8601(date));

        // 測試 DateTime
        org.joda.time.DateTime dateTime = new org.joda.time.DateTime();
        System.out.println("ISO8601格式(DateTime): " + toIso8601(dateTime));

        // 測試特定時間的 ISO8601 格式
        org.joda.time.DateTime specificTime = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45, 123);
        System.out.println("特定時間: " + specificTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("特定時間ISO8601: " + toIso8601(specificTime));

        // 測試 null 值
        System.out.println("ISO8601格式(null Date): " + toIso8601((java.util.Date) null));
        System.out.println("ISO8601格式(null DateTime): " + toIso8601((org.joda.time.DateTime) null));

        // 測試 ISO8601 解析方法
        System.out.println("\n========== ISO8601 解析測試 ==========");

        // 測試解析 ISO8601 字串
        String iso8601String = toIso8601(specificTime);
        System.out.println("原始ISO8601字串: " + iso8601String);

        // 解析為時間戳
        long parsedTimestamp = parseIso8601ToTimestamp(iso8601String);
        System.out.println("解析為時間戳: " + parsedTimestamp);
        System.out.println("原始時間戳: " + specificTime.getMillis());
        System.out.println("時間戳是否相等: " + (parsedTimestamp == specificTime.getMillis()));

        // 解析為 Date
        java.util.Date parsedDate = parseIso8601ToDate(iso8601String);
        System.out.println("解析為Date: " + formatDate(parsedDate, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        // 解析為 DateTime
        org.joda.time.DateTime parsedDateTime = parseIso8601ToDateTime(iso8601String);
        System.out.println("解析為DateTime: " + parsedDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("DateTime是否相等: " + specificTime.equals(parsedDateTime));

        // 解析為 LocalDate
        org.joda.time.LocalDate parsedLocalDate = parseIso8601ToLocalDate(iso8601String);
        System.out.println("解析為LocalDate: "
                + (parsedLocalDate != null ? parsedLocalDate.toString(DATE_FORMAT_YYYY_MM_DD) : "null"));
        System.out.println(
                "LocalDate是否相等: " + (parsedLocalDate != null && specificTime.toLocalDate().equals(parsedLocalDate)));

        // 測試各種 ISO8601 格式
        String[] testIso8601Strings = {
                "2025-08-14T15:30:45.123+0800",
                "2025-08-14T15:30:45.123Z",
                "2025-08-14T15:30:45+0800",
                "2025-08-14T15:30:45Z",
                "2025-12-25T00:00:00.000+0800"
        };

        System.out.println("\n--- 測試各種 ISO8601 格式 ---");
        for (String testStr : testIso8601Strings) {
            System.out.println("測試字串: " + testStr);
            long testTimestamp = parseIso8601ToTimestamp(testStr);
            if (testTimestamp > 0) {
                System.out.println("  解析成功: " + formatTimestamp(testTimestamp, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
            } else {
                System.out.println("  解析失敗");
            }
        }

        // 測試錯誤輸入
        System.out.println("\n--- 測試錯誤輸入 ---");
        String[] errorInputs = { null, "", "invalid-date", "2025-13-01T25:00:00Z" };
        for (String errorInput : errorInputs) {
            System.out.println("錯誤輸入: " + errorInput);
            long errorTimestamp = parseIso8601ToTimestamp(errorInput);
            java.util.Date errorDate = parseIso8601ToDate(errorInput);
            org.joda.time.DateTime errorDateTime = parseIso8601ToDateTime(errorInput);
            org.joda.time.LocalDate errorLocalDate = parseIso8601ToLocalDate(errorInput);
            System.out.println("  時間戳: " + errorTimestamp);
            System.out.println("  Date: " + errorDate);
            System.out.println("  DateTime: " + errorDateTime);
            System.out.println("  LocalDate: " + errorLocalDate);
        }

        // 測試 LocalDate 的 ISO8601 格式化
        System.out.println("\n--- 測試 LocalDate ISO8601 格式化 ---");
        org.joda.time.LocalDate testLocalDate = new org.joda.time.LocalDate(2025, 8, 14);
        String localDateIso8601 = toIso8601(testLocalDate);
        System.out.println("LocalDate: " + testLocalDate.toString(DATE_FORMAT_YYYY_MM_DD));
        System.out.println("轉為ISO8601: " + localDateIso8601);

        // 測試往返轉換
        org.joda.time.LocalDate reparsedFromIso = parseIso8601ToLocalDate(localDateIso8601);
        System.out.println(
                "重新解析: " + (reparsedFromIso != null ? reparsedFromIso.toString(DATE_FORMAT_YYYY_MM_DD) : "null"));
        System.out.println("往返轉換一致: " + (reparsedFromIso != null && testLocalDate.equals(reparsedFromIso)));

        // 測試 null 輸入
        String nullLocalDateIso = toIso8601((org.joda.time.LocalDate) null);
        System.out.println("null LocalDate 轉ISO8601: " + nullLocalDateIso);

        // 測試普通時間字串解析方法
        System.out.println("\n========== 普通時間字串解析測試 ==========");

        // 測試各種格式的時間字串
        String[] testDateStrings = {
                "2025-08-14 15:30:45",
                "2025-08-14 15:30:45.123",
                "2025-08-14",
                "2025-12-25 00:00:00",
                "2024-02-29 23:59:59"
        };

        System.out.println("--- 測試各種普通時間格式 ---");
        for (String testStr : testDateStrings) {
            System.out.println("測試字串: " + testStr);

            // 測試解析為時間戳
            long testTimestamp = parseToTimestamp(testStr);
            if (testTimestamp > 0) {
                System.out.println("  解析為時間戳: " + testTimestamp + " -> "
                        + formatTimestamp(testTimestamp, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
            } else {
                System.out.println("  解析時間戳失敗");
            }

            // 測試解析為 Date
            java.util.Date normalParsedDate = parseToDate(testStr);
            if (normalParsedDate != null) {
                System.out.println("  解析為Date: " + formatDate(normalParsedDate, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
            } else {
                System.out.println("  解析Date失敗");
            }

            // 測試解析為 DateTime
            org.joda.time.DateTime normalParsedDateTime = parseToDateTime(testStr);
            if (normalParsedDateTime != null) {
                System.out.println("  解析為DateTime: " + normalParsedDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
            } else {
                System.out.println("  解析DateTime失敗");
            }

            // 測試解析為 LocalDate
            org.joda.time.LocalDate normalParsedLocalDate = parseToLocalDate(testStr);
            if (normalParsedLocalDate != null) {
                System.out.println("  解析為LocalDate: " + formatLocalDate(normalParsedLocalDate, DATE_FORMAT_YYYY_MM_DD));
            } else {
                System.out.println("  解析LocalDate失敗");
            }
            System.out.println();
        }

        // 測試往返轉換準確性
        System.out.println("--- 測試往返轉換準確性 ---");
        org.joda.time.DateTime originalDateTime = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45, 123);

        // DateTime -> 格式化字串 -> 重新解析
        String formattedStr = originalDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
        org.joda.time.DateTime reparsedDateTime = parseToDateTime(formattedStr);
        System.out.println("原始DateTime: " + originalDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("格式化字串: " + formattedStr);
        System.out.println("重新解析: "
                + (reparsedDateTime != null ? reparsedDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS) : "null"));
        System.out.println("日期部分是否一致: "
                + (reparsedDateTime != null && originalDateTime.toLocalDate().equals(reparsedDateTime.toLocalDate())));

        // 測試帶毫秒的往返轉換
        String formattedWithMillis = originalDateTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_SSS);
        org.joda.time.DateTime parsedWithMillis = parseToDateTime(formattedWithMillis);
        System.out.println("\n帶毫秒格式化: " + formattedWithMillis);
        System.out.println("重新解析: "
                + (parsedWithMillis != null ? parsedWithMillis.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_SSS) : "null"));
        System.out.println("完全一致: " + (parsedWithMillis != null && originalDateTime.equals(parsedWithMillis)));

        // 測試普通格式的錯誤輸入
        System.out.println("\n--- 測試普通格式錯誤輸入 ---");
        String[] errorDateStrings = { null, "", "invalid-date", "2025-13-01 25:00:00", "2025/08/14", "14-08-2025" };
        for (String errorInput : errorDateStrings) {
            System.out.println("錯誤輸入: " + errorInput);
            long errorTimestamp = parseToTimestamp(errorInput);
            java.util.Date errorDate = parseToDate(errorInput);
            org.joda.time.DateTime errorDateTime = parseToDateTime(errorInput);
            org.joda.time.LocalDate errorLocalDate = parseToLocalDate(errorInput);
            System.out.println("  時間戳: " + errorTimestamp);
            System.out.println("  Date: " + errorDate);
            System.out.println("  DateTime: " + errorDateTime);
            System.out.println("  LocalDate: " + errorLocalDate);
        }

        // 測試時間轉字串方法
        System.out.println("\n========== 時間轉字串測試 ==========");

        // 測試當前時間的各種轉換
        long currentTimestampForTimeStr = getCurrentTimestamp();
        java.util.Date currentDateForTimeStr = new java.util.Date();
        org.joda.time.LocalDate currentLocalDateForTimeStr = org.joda.time.LocalDate.now();
        org.joda.time.DateTime currentDateTimeForTimeStr = org.joda.time.DateTime.now();

        System.out.println("--- 測試當前時間轉換 ---");
        System.out.println("時間戳轉字串: " + toTimeStr(currentTimestampForTimeStr));
        System.out.println("Date轉字串: " + toTimeStr(currentDateForTimeStr));
        System.out.println("LocalDate轉字串: " + toTimeStr(currentLocalDateForTimeStr));
        System.out.println("DateTime轉字串: " + toTimeStr(currentDateTimeForTimeStr));

        // 測試特定時間的轉換
        System.out.println("\n--- 測試特定時間轉換 ---");
        long specificTimestamp = 1755156645123L; // 2025-08-14 15:30:45.123
        java.util.Date specificDate = new java.util.Date(specificTimestamp);
        org.joda.time.LocalDate specificLocalDate = new org.joda.time.LocalDate(2025, 8, 14);
        org.joda.time.DateTime specificDateTime = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45, 123);

        System.out.println("特定時間戳: " + specificTimestamp);
        System.out.println("時間戳轉字串: " + toTimeStr(specificTimestamp));
        System.out.println("Date轉字串: " + toTimeStr(specificDate));
        System.out.println("LocalDate轉字串: " + toTimeStr(specificLocalDate));
        System.out.println("DateTime轉字串: " + toTimeStr(specificDateTime));

        // 測試各種邊界情況
        System.out.println("\n--- 測試邊界情況 ---");

        // 測試年初
        org.joda.time.DateTime newYear = new org.joda.time.DateTime(2025, 1, 1, 0, 0, 0);
        System.out.println("新年開始: " + toTimeStr(newYear));

        // 測試年末
        org.joda.time.DateTime yearEnd = new org.joda.time.DateTime(2025, 12, 31, 23, 59, 59);
        System.out.println("年末結束: " + toTimeStr(yearEnd));

        // 測試閏年
        org.joda.time.DateTime leapDay = new org.joda.time.DateTime(2024, 2, 29, 12, 0, 0);
        System.out.println("閏年2月29日: " + toTimeStr(leapDay));

        // 測試 null 值
        System.out.println("\n--- 測試 null 值 ---");
        System.out.println("null Date: " + toTimeStr((java.util.Date) null));
        System.out.println("null LocalDate: " + toTimeStr((org.joda.time.LocalDate) null));
        System.out.println("null DateTime: " + toTimeStr((org.joda.time.DateTime) null));

        // 測試與解析方法的往返轉換
        System.out.println("\n--- 測試往返轉換一致性 ---");
        org.joda.time.DateTime originalTime = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45);
        String timeStr = toTimeStr(originalTime);
        org.joda.time.DateTime reparsedTime = parseToDateTime(timeStr);

        System.out.println("原始時間: " + originalTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("轉為字串: " + timeStr);
        System.out.println(
                "重新解析: " + (reparsedTime != null ? reparsedTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS) : "null"));
        System.out.println("往返轉換一致: " + (reparsedTime != null && originalTime.equals(reparsedTime)));

        // 測試 LocalDate 的往返轉換
        org.joda.time.LocalDate originalLocalDate = new org.joda.time.LocalDate(2025, 8, 14);
        String localDateStr = toTimeStr(originalLocalDate);
        org.joda.time.LocalDate reparsedLocalDate = parseToLocalDate(localDateStr);

        System.out.println("\nLocalDate往返轉換:");
        System.out.println("原始LocalDate: " + originalLocalDate.toString(DATE_FORMAT_YYYY_MM_DD));
        System.out.println("轉為字串: " + localDateStr);
        System.out.println(
                "重新解析: " + (reparsedLocalDate != null ? reparsedLocalDate.toString(DATE_FORMAT_YYYY_MM_DD) : "null"));
        System.out.println("往返轉換一致: " + (reparsedLocalDate != null && originalLocalDate.equals(reparsedLocalDate)));

        // 測試時間差計算方法
        System.out.println("\n========== 時間差計算測試 ==========");

        // 測試基準時間
        org.joda.time.DateTime baseTime = new org.joda.time.DateTime(2025, 8, 14, 15, 30, 45);
        org.joda.time.DateTime futureTime = baseTime.plusYears(2).plusMonths(6).plusDays(15).plusHours(8)
                .plusMinutes(30).plusSeconds(45);
        org.joda.time.DateTime pastTime = baseTime.minusYears(1).minusMonths(3).minusDays(10).minusHours(5)
                .minusMinutes(20).minusSeconds(30);

        System.out.println("基準時間: " + baseTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("未來時間: " + futureTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));
        System.out.println("過去時間: " + pastTime.toString(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS));

        System.out.println("\n--- 基準時間 vs 未來時間 ---");
        System.out.println("年份差: " + betweenYears(baseTime, futureTime) + " (負數表示基準時間較早)");
        System.out.println("月份差: " + betweenMonths(baseTime, futureTime));
        System.out.println("週數差: " + betweenWeeks(baseTime, futureTime));
        System.out.println("天數差: " + betweenDays(baseTime, futureTime));
        System.out.println("小時差: " + betweenHours(baseTime, futureTime));
        System.out.println("分鐘差: " + betweenMinutes(baseTime, futureTime));
        System.out.println("秒數差: " + betweenSeconds(baseTime, futureTime));

        System.out.println("\n--- 基準時間 vs 過去時間 ---");
        System.out.println("年份差: " + betweenYears(baseTime, pastTime) + " (正數表示基準時間較晚)");
        System.out.println("月份差: " + betweenMonths(baseTime, pastTime));
        System.out.println("週數差: " + betweenWeeks(baseTime, pastTime));
        System.out.println("天數差: " + betweenDays(baseTime, pastTime));
        System.out.println("小時差: " + betweenHours(baseTime, pastTime));
        System.out.println("分鐘差: " + betweenMinutes(baseTime, pastTime));
        System.out.println("秒數差: " + betweenSeconds(baseTime, pastTime));

        System.out.println("\n--- 反向測試（未來時間 vs 基準時間）---");
        System.out.println("年份差: " + betweenYears(futureTime, baseTime) + " (正數表示未來時間較晚)");
        System.out.println("月份差: " + betweenMonths(futureTime, baseTime));

        // 測試 timestamp 和 Date 類型
        System.out.println("\n--- 時間戳測試 ---");
        long baseTimestamp = baseTime.getMillis();
        long futureTimestamp = futureTime.getMillis();
        System.out.println("時間戳年份差: " + betweenYears(baseTimestamp, futureTimestamp));
        System.out.println("時間戳天數差: " + betweenDays(baseTimestamp, futureTimestamp));

        java.util.Date baseDate = new java.util.Date(baseTimestamp);
        java.util.Date futureDate = new java.util.Date(futureTimestamp);
        System.out.println("Date年份差: " + betweenYears(baseDate, futureDate));
        System.out.println("Date天數差: " + betweenDays(baseDate, futureDate));
    }
}
