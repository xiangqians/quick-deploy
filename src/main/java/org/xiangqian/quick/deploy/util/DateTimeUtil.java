package org.xiangqian.quick.deploy.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期时间工具类
 * <p>
 * {@link  java.time.LocalDateTime} 本地日期时间：表示了一个没有时区信息的日期和时间，并且它的内部实现是基于本地时钟的，不涉及时区转换。因此，{@link LocalDateTime} 对象的创建和操作通常比较高效。
 * <p>
 * {@link  java.time.ZonedDateTime} 时区日期时间：类表示了一个带有时区信息的日期和时间。它提供了更多的功能，例如时区转换、跨时区计算等。由于其涉及到时区的处理，创建和操作 {@link ZonedDateTime} 对象可能会比 {@link LocalDateTime} 稍微慢一些。然而，这个性能差异通常是微小的，对于大多数情况来说不会对性能产生显著影响。
 * <p>
 * 日期时间格式
 * "yyyy"：年份
 * "MM"  ：月份
 * "dd"  ：日期
 * "HH"  ：小时（24小时制）
 * "mm"  ：分钟
 * "ss"  ：秒钟
 * "SSS" ：毫秒
 * 例如：
 * "yyyy/MM/dd"：年份、月份和日期（例如：2023/11/13）
 * "HH:mm:ss"  ：小时、分钟和秒钟（例如：10:31:38）
 * "yyyy/MM/dd HH:mm:ss.SSS"：完整的日期时间（例如：2023/11/13 10:31:38.491）
 *
 * @author xiangqian
 * @date 2026/01/19 15:09
 */
public class DateTimeUtil {

    private static final Map<String, DateTimeFormatter> FORMATTERS = new HashMap<>(4);

    private static synchronized DateTimeFormatter getFormatter(String pattern) {
        DateTimeFormatter formatter = FORMATTERS.get(pattern);
        if (formatter == null) {
            formatter = DateTimeFormatter.ofPattern(pattern);
            FORMATTERS.put(pattern, formatter);
        }
        return formatter;
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return format(dateTime, "yyyy/MM/dd HH:mm:ss");
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(getFormatter(pattern));
    }

    /**
     * 人性化日期时间
     *
     * @param dateTime
     * @return
     */
    public static String human(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        if (duration.compareTo(Duration.ZERO) <= 0) {
            return "0秒前";
        }

        // 天
        if (duration.toDays() > 0) {
            return DateTimeUtil.format(dateTime);
        }

        // 小时
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + "小时前";
        }

        // 分钟
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + "分钟前";
        }

        // 秒
        long seconds = duration.toSeconds();
        return seconds + "秒前";
    }

}
