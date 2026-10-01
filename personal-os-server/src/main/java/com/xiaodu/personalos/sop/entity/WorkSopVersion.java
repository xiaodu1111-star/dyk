package com.xiaodu.personalos.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 版本历史实体（表 {@code work_sop_version}，Flyway V16）。
 *
 * <p>{@code content_json} 为 JSON 列，Java 侧以 String 承载，读写时自行序列化/反序列化。</p>
 *
 * @author Alex
 */
@Data
@TableName("work_sop_version")
public class WorkSopVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 SOP id。 */
    private Long sopId;

    /** 版本号。 */
    private String version;

    /** 该版本的完整快照（含步骤）JSON 字符串。 */
    private String contentJson;

    /** 本次优化了什么。 */
    private String changeNote;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
