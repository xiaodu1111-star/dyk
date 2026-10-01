package com.xiaodu.personalos.system;

import com.xiaodu.personalos.common.util.PeriodUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PeriodUtil 时间口径单测（无 Spring 上下文）。
 *
 * <p>口径样本取自《m0-infra.md》：2026-10-01 为周四、周 key 2026-W40、月 key 2026-10。</p>
 *
 * @author Kou
 */
class PeriodUtilTest {

    /** 规格样本：2026-10-01（周四）→ 周 key 2026-W40、月 key 2026-10、中文周四。 */
    @Test
    void specSample20261001() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        assertEquals("2026-W40", PeriodUtil.weekKey(date));
        assertEquals("2026-10", PeriodUtil.monthKey(date));
        assertEquals("周四", PeriodUtil.dayOfWeekCn(date));
        assertEquals("2026-10-01", PeriodUtil.formatDate(date));
    }

    /** 周首周一：周四所在周的周一 = 2026-09-28，周日 = 2026-10-04。 */
    @Test
    void weekStartsOnMonday() {
        LocalDate thursday = LocalDate.of(2026, 10, 1);
        assertEquals(LocalDate.of(2026, 9, 28), PeriodUtil.weekStart(thursday));
        assertEquals(LocalDate.of(2026, 10, 4), PeriodUtil.weekEnd(thursday));
        // 周日仍属同一周，周首还是 2026-09-28
        assertEquals(LocalDate.of(2026, 9, 28), PeriodUtil.weekStart(LocalDate.of(2026, 10, 4)));
        // 下周一开新周
        assertEquals(LocalDate.of(2026, 10, 5), PeriodUtil.weekStart(LocalDate.of(2026, 10, 5)));
    }

    /** ISO 跨年边界：2026 有 53 个 ISO 周，2027-01-01（周五）仍属 2026-W53。 */
    @Test
    void isoYearBoundaryBelongsToWeekBasedYear() {
        assertEquals("2026-W53", PeriodUtil.weekKey(LocalDate.of(2026, 12, 31)));
        assertEquals("2026-W53", PeriodUtil.weekKey(LocalDate.of(2027, 1, 1)));
        assertEquals("2027-W01", PeriodUtil.weekKey(LocalDate.of(2027, 1, 4)));
    }

    /** 周 key / 月 key 互转与校验。 */
    @Test
    void keyParseRoundTrip() {
        assertEquals(LocalDate.of(2026, 9, 28), PeriodUtil.weekKeyStart("2026-W40"));
        assertEquals(LocalDate.of(2026, 10, 1), PeriodUtil.monthKeyStart("2026-10"));

        assertTrue(PeriodUtil.isWeekKey("2026-W40"));
        assertFalse(PeriodUtil.isWeekKey("2026-10"));
        assertTrue(PeriodUtil.isMonthKey("2026-10"));
        assertFalse(PeriodUtil.isMonthKey("2026-W40"));
        assertFalse(PeriodUtil.isMonthKey("2026-13"));
    }

    /** periodRange：周 key → [周一, 周日]；月 key → [月初, 月末]。 */
    @Test
    void periodRangeCoversWholePeriod() {
        assertArrayEquals(
                new LocalDate[]{LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)},
                PeriodUtil.periodRange("2026-W40"));
        assertArrayEquals(
                new LocalDate[]{LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)},
                PeriodUtil.periodRange("2026-10"));
        assertThrows(IllegalArgumentException.class, () -> PeriodUtil.periodRange("2026"));
        assertThrows(IllegalArgumentException.class, () -> PeriodUtil.periodRange("2026-W99"));
    }

    /** 自然日 00:00 分界：当前口径下归属日即自然日；常量位已预留 04:00。 */
    @Test
    void naturalDayCutoff() {
        assertEquals(0, PeriodUtil.DAY_CUTOFF_HOUR);
        // 2026-10-01 23:59 仍归属 10-01
        assertEquals(LocalDate.of(2026, 10, 1),
                PeriodUtil.effectiveDate(LocalDateTime.of(2026, 10, 1, 23, 59)));
        // 归属日起止
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), PeriodUtil.dayStart(LocalDate.of(2026, 10, 1)));
        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), PeriodUtil.dayEnd(LocalDate.of(2026, 10, 1)));
    }
}
