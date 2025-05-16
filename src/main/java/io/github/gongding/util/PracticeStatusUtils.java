package io.github.gongding.util;

import java.time.LocalDateTime;

public class PracticeStatusUtils {
    /**
     * 根据开始时间和结束时间计算当前练习的状态
     * @param startTime 练习的开始时间
     * @param endTime   练习的结束时间
     * @return 返回表示练习状态的字符串 ("not_started","in_progress","ended")
     */
    public static String calculateStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "not_started";
        } else if (now.isAfter(startTime) && now.isBefore(endTime)) {
            return "in_progress";
        } else {
            return "ended";
        }
    }
}