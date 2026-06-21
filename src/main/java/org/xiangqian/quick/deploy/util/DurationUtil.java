package org.xiangqian.quick.deploy.util;

import java.time.Duration;

/**
 * 时长工具类
 *
 * @author xiangqian
 * @date 2026/04/03 15:32
 */
public class DurationUtil {

    /**
     * 人性化持续时间
     *
     * @param duration
     * @return
     */
    public static String human(Duration duration) {
        if (duration == null) {
            return "-";
        }

        if (duration.compareTo(Duration.ZERO) <= 0) {
            return "0秒";
        }

        StringBuilder builder = new StringBuilder();

        // 小时
        long hours = duration.toHours();
        if (hours > 0) {
            builder.append(hours).append("小时");
        }

        // 分钟
        int minutes = duration.toMinutesPart();
        if (minutes > 0) {
            builder.append(minutes).append("分钟");
        }

        // 秒
        int seconds = duration.toSecondsPart();
        builder.append(seconds).append("秒");

        return builder.toString();
    }

}
