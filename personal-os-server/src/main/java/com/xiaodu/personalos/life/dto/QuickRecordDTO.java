package com.xiaodu.personalos.life.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 快捷记录解析入参（M3）。
 *
 * <p>格式：{@code <指标名前缀> [数字]}，如「跑步 5」「喝 8」。
 * 该接口<b>只解析不落库</b>（总设计红线）。</p>
 *
 * @author Kou
 */
@Data
public class QuickRecordDTO {

    /** 待解析文本，如「跑步 5」。 */
    @NotBlank(message = "记录文本不能为空")
    @Size(max = 100, message = "记录文本最长 100 字")
    private String text;
}
