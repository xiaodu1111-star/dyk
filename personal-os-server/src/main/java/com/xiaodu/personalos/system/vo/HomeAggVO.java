package com.xiaodu.personalos.system.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页聚合 VO（契约对齐《m0-infra.md》§3，M0 全骨架值，P1 集成阶段各模块接线）。
 *
 * @author Kou
 */
@Data
public class HomeAggVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 今日信息。 */
    private TodayVO today = new TodayVO();

    /** 工作域概况（M1 接线）。 */
    private WorkVO work = new WorkVO();

    /** 生活域概况（M3 接线）。 */
    private LifeVO life = new LifeVO();

    /** SOP 域概况（M2 接线）。 */
    private SopVO sop = new SopVO();

    /** 连续打卡天数（M3 接线）。 */
    private int streakDays;

    /** SOP 使用提示（M2 接线）。 */
    private List<SopHintVO> sopHints = new ArrayList<>();

    /** 今日信息。 */
    @Data
    public static class TodayVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** yyyy-MM-dd。 */
        private String date;

        /** 中文星期，如 周四。 */
        private String week;
    }

    /** 工作域概况。 */
    @Data
    public static class WorkVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 今日任务总数。 */
        private int todayTotal;

        /** 今日已完成数。 */
        private int todayDone;

        /** 逾期数。 */
        private int overdue;
    }

    /** 生活域概况。 */
    @Data
    public static class LifeVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 今日已打卡数。 */
        private int checkinDone;

        /** 今日应打卡数。 */
        private int checkinTotal;

        /** 习惯列表。 */
        private List<HabitVO> habits = new ArrayList<>();
    }

    /** 习惯条目。 */
    @Data
    public static class HabitVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        /** 习惯名。 */
        private String name;

        /** 今日是否已打卡。 */
        private boolean done;
    }

    /** SOP 域概况。 */
    @Data
    public static class SopVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 高频 SOP 列表。 */
        private List<SopItemVO> top = new ArrayList<>();
    }

    /** SOP 条目。 */
    @Data
    public static class SopItemVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        /** 标题。 */
        private String title;

        /** 使用次数。 */
        private int useCount;
    }

    /** SOP 使用提示。 */
    @Data
    public static class SopHintVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 任务标题。 */
        private String taskTitle;

        /** 近期出现次数。 */
        private int count;
    }
}
