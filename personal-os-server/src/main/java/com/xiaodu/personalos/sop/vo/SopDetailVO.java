package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SOP 详情 VO。
 *
 * <p>SOP 主字段 + {@code steps[]} + {@code versions[]} 摘要 + {@code stats}。</p>
 *
 * @author Alex
 */
@Data
public class SopDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** SOP id。 */
    private Long id;

    /** 标题。 */
    private String title;

    /** 分类。 */
    private String category;

    /** 使用场景。 */
    private String triggerScene;

    /** 产出目标。 */
    private String goal;

    /** 版本号。 */
    private String version;

    /** 被复用次数。 */
    private Integer useCount;

    /** 平均耗时（分钟）。 */
    private Integer avgMinutes;

    /** 最近使用时间。 */
    private LocalDateTime lastUsedAt;

    /** 状态。 */
    private String status;

    /** 是否置顶。 */
    private Integer pinned;

    /** 沉淀来源任务 id。 */
    private Long sourceTaskId;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 步骤清单（按 step_no 升序）。 */
    private List<SopStepVO> steps = new ArrayList<>();

    /** 版本时间线摘要（倒序）。 */
    private List<SopVersionVO> versions = new ArrayList<>();

    /** 执行统计。 */
    private SopStatsVO stats = new SopStatsVO();
}
