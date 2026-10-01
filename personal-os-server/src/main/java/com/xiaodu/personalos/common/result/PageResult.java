package com.xiaodu.personalos.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页结果。
 *
 * @param <T> 记录类型
 * @author Kou
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页记录。 */
    private List<T> records = new ArrayList<>();

    /** 总记录数。 */
    private long total;

    /** 当前页码（从 1 开始）。 */
    private long page;

    /** 每页条数。 */
    private long size;

    public PageResult(List<T> records, long total, long page, long size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 由 MyBatis-Plus 的 {@link IPage} 构建。 */
    public static <T> PageResult<T> of(IPage<T> p) {
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }
}
