package com.xiaodu.personalos.sop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SOP 创建 / 编辑入参。
 *
 * <p>创建与编辑共用同一结构：编辑时步骤为覆盖式全量替换。
 * 步骤的 step_no 由服务端按顺序重排为 1..N，入参无需携带。</p>
 *
 * @author Alex
 */
@Data
public class SopSaveDTO {

    /** 标题（必填）。 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    private String title;

    /** 分类，如 需求评审/故障处理/周报。 */
    @Size(max = 50, message = "分类最长 50 字")
    private String category;

    /** 什么场景下用这个 SOP。 */
    @Size(max = 500, message = "使用场景最长 500 字")
    private String triggerScene;

    /** 产出什么结果。 */
    @Size(max = 500, message = "目标最长 500 字")
    private String goal;

    /** 由哪个任务沉淀而来（"从任务沉淀"入口，可选）。 */
    private Long sourceTaskId;

    /** 状态：draft/active/deprecated，可选，默认沿用既有或 draft。 */
    @Size(max = 20, message = "状态最长 20 字")
    private String status;

    /** 是否置顶：0 否 / 1 是，可选。 */
    private Integer pinned;

    /** 版本变更说明（编辑时可选，写入快照的 change_note）。 */
    @Size(max = 500, message = "变更说明最长 500 字")
    private String changeNote;

    /** 步骤列表（可选；编辑时全量替换）。 */
    @Valid
    private List<SopStepDTO> steps = new ArrayList<>();
}
