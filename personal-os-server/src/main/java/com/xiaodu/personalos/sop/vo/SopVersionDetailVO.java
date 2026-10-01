package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SOP 版本完整快照 VO（回看用，含 contentJson 解析后的内容）。
 *
 * @author Alex
 */
@Data
public class SopVersionDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 版本记录 id。 */
    private Long id;

    /** 所属 SOP id。 */
    private Long sopId;

    /** 版本号。 */
    private String version;

    /** 本次优化了什么。 */
    private String changeNote;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 快照时的标题。 */
    private String title;

    /** 快照时的分类。 */
    private String category;

    /** 快照时的使用场景。 */
    private String triggerScene;

    /** 快照时的目标。 */
    private String goal;

    /** 快照时的状态。 */
    private String status;

    /** 快照时的步骤清单。 */
    private List<SopStepVO> steps = new ArrayList<>();

    /** 原始快照 JSON（前端如需自行渲染）。 */
    private String contentJson;
}
