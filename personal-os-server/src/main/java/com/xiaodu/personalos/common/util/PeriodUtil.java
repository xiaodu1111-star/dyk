package com.xiaodu.personalos.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.regex.Pattern;

/**
 * 时间口径唯一来源（M0 基础设施，对齐《m0-infra.md》交付项 4）。
 *
 * <p>全系统口径约定：</p>
 * <ul>
 *   <li>自然日 00:00 分界（预留 04:00 口径切换常量位，本期不实现）</li>
 *   <li>周一为一周之首（{@link WeekFields#ISO}）</li>
 *   <li>周 key 形如 {@code 2026-W40}，月 key 形如 {@code 2026-10}</li>
 * </ul>
 *
 * <p><b>严禁</b>各业务模块自行计算周期边界 / 周 key / 月 key，一律经本工具类，
 * 保证口径切换时只改一处。</p>
 *
 * @author Kou
 */
public final class PeriodUtil {

    /** 日分界小时。当前口径：自然日 00:00。预留 04:00 切换位，改此常量即全局切换。 */
    public static final int DAY_CUTOFF_HOUR = 0;

    /** 周 key 格式：2026-W40。 */
    private static final Pattern WEEK_KEY_PATTERN = Pattern.compile("^\\d{4}-W\\d{2}$");

    /** 月 key 格式：2026-10。 */
    private static final Pattern MONTH_KEY_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    /** ISO 周字段（周一为周首）。 */
    private static final WeekFields ISO_WEEK = WeekFields.ISO;

    /** 中文星期名，下标 = DayOfWeek.getValue() - 1（周一 = 1）。 */
    private static final String[] DAY_OF_WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private PeriodUtil() {
    }

    // ---- 基础口径 -------------------------------------------------------

    /**
     * 事件归属日：按日分界常量折算。当前口径（00:00 分界）即自然日；
     * 若切到 04:00 口径，凌晨 0-3 点的事件自动归属前一天。
     *
     * @param dateTime 事件发生时刻
     * @return 归属日
     */
    public static LocalDate effectiveDate(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        return dateTime.getHour() < DAY_CUTOFF_HOUR ? date.minusDays(1) : date;
    }

    /** 今天（按当前时刻折算归属日）。 */
    public static LocalDate today() {
        return effectiveDate(LocalDateTime.now());
    }

    /** 归属日的统计起点（含）。 */
    public static LocalDateTime dayStart(LocalDate date) {
        return date.atStartOfDay().plusHours(DAY_CUTOFF_HOUR);
    }

    /** 归属日的统计终点（不含）。 */
    public static LocalDateTime dayEnd(LocalDate date) {
        return dayStart(date).plusDays(1);
    }

    // ---- 周 / 月 key ----------------------------------------------------

    /** 周 key：{@code 2026-W40}（ISO 周年 + ISO 周号，跨年周归属以周年为准）。 */
    public static String weekKey(LocalDate date) {
        int weekBasedYear = date.get(ISO_WEEK.weekBasedYear());
        int weekOfWeekBasedYear = date.get(ISO_WEEK.weekOfWeekBasedYear());
        return String.format("%04d-W%02d", weekBasedYear, weekOfWeekBasedYear);
    }

    /** 月 key：{@code 2026-10}。 */
    public static String monthKey(LocalDate date) {
        return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    }

    /** 是否为合法周 key（{@code 2026-W40}）。 */
    public static boolean isWeekKey(String key) {
        return key != null && WEEK_KEY_PATTERN.matcher(key).matches();
    }

    /** 是否为合法月 key（{@code 2026-10}）。 */
    public static boolean isMonthKey(String key) {
        return key != null && MONTH_KEY_PATTERN.matcher(key).matches();
    }

    // ---- 周期边界 -------------------------------------------------------

    /** 该日所在周的周一。 */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 该日所在周的周日。 */
    public static LocalDate weekEnd(LocalDate date) {
        return weekStart(date).plusDays(6);
    }

    /** 该日所在月的第一天。 */
    public static LocalDate monthStart(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /** 该日所在月的最后一天。 */
    public static LocalDate monthEnd(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    /** 周 key → 该周周一。非法 key 抛 {@link IllegalArgumentException}。 */
    public static LocalDate weekKeyStart(String weekKey) {
        if (!isWeekKey(weekKey)) {
            throw new IllegalArgumentException("非法周 key: " + weekKey);
        }
        int weekBasedYear = Integer.parseInt(weekKey.substring(0, 4));
        int week = Integer.parseInt(weekKey.substring(6));
        if (week < 1 || week > 53) {
            throw new IllegalArgumentException("非法周号: " + weekKey);
        }
        // 1 月 4 日恒在 ISO 第 1 周，以此为锚点跳到目标周
        LocalDate anchor = LocalDate.of(weekBasedYear, 1, 4);
        return anchor.with(ISO_WEEK.weekOfWeekBasedYear(), week)
                .with(ISO_WEEK.dayOfWeek(), 1);
    }

    /** 月 key → 该月第一天。非法 key 抛 {@link IllegalArgumentException}。 */
    public static LocalDate monthKeyStart(String monthKey) {
        if (!isMonthKey(monthKey)) {
            throw new IllegalArgumentException("非法月 key: " + monthKey);
        }
        return LocalDate.of(
                Integer.parseInt(monthKey.substring(0, 4)),
                Integer.parseInt(monthKey.substring(5)), 1);
    }

    /**
     * 周期 key（周或月）→ [起, 止] 闭区间。
     *
     * @param key 周 key（2026-W40）或月 key（2026-10）
     * @return 长度为 2 的数组，[0]=起始日，[1]=结束日
     * @throws IllegalArgumentException key 既非周 key 也非月 key
     */
    public static LocalDate[] periodRange(String key) {
        if (isWeekKey(key)) {
            LocalDate start = weekKeyStart(key);
            return new LocalDate[]{start, start.plusDays(6)};
        }
        if (isMonthKey(key)) {
            LocalDate start = monthKeyStart(key);
            return new LocalDate[]{start, monthEnd(start)};
        }
        throw new IllegalArgumentException("非法周期 key: " + key);
    }

    // ---- 展示辅助 -------------------------------------------------------

    /** 中文星期：周四。 */
    public static String dayOfWeekCn(LocalDate date) {
        return DAY_OF_WEEK_CN[date.getDayOfWeek().getValue() - 1];
    }

    /** yyyy-MM-dd。 */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}
