package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 列表卡 VO。
 *
 * <p>契约：{@code { id, title, category, triggerScene, goal, version, useCount,
 * avgMinutes, lastUsedAt, status, pinned, stepCount, createTime, updateTime }}。
 * 卡片直接露出 {@code useCount}（大数字）与 {@code avgMinutes}（次级文字）。</p>
 *
 * @author Alex
 */
@Data
public class SopVO implements Serializable {

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

    /** 被复用次数（卡片大数字）。 */
    private Integer useCount;

    /** 平均耗时（分钟，卡片次级文字）。 */
    private Integer avgMinutes;

    /** 最近使用时间。 */
    private LocalDateTime lastUsedAt;

    /** 状态。 */
    private String status;

    /** 是否置顶。 */
    private Integer pinned;

    /** 步骤数。 */
    private Integer stepCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
