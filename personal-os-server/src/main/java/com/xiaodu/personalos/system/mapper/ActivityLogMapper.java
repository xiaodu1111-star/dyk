package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.ActActivityLog;
import com.xiaodu.personalos.system.vo.HomeAggVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 统一活动流 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface ActivityLogMapper extends BaseMapper<ActActivityLog> {

    /**
     * 统计窗口期内重复出现的活动标题（用于首页 SOP 提示：同一件事反复做，值得沉淀成 SOP）。
     *
     * <p>只读聚合查询，不参与写入链路。</p>
     *
     * @param userId    用户 id
     * @param dimension 维度（work / life / …）
     * @param bizType   业务类型（task_done / checkin / …）
     * @param sinceDate 统计窗口起始日（含）
     * @param minCount  最少出现次数（低于此值不返回）
     * @param limit     最多返回条数
     * @return 标题 + 出现次数，按次数倒序
     */
    @Select("""
            SELECT title AS task_title, COUNT(*) AS title_count
            FROM act_activity_log
            WHERE user_id = #{userId}
              AND dimension = #{dimension}
              AND biz_type = #{bizType}
              AND activity_date >= #{sinceDate}
            GROUP BY title
            HAVING COUNT(*) >= #{minCount}
            ORDER BY title_count DESC, title ASC
            LIMIT #{limit}
            """)
    @Results({
            @Result(column = "task_title", property = "taskTitle"),
            @Result(column = "title_count", property = "count")
    })
    List<HomeAggVO.SopHintVO> countRepeatedTitles(@Param("userId") long userId,
                                                 @Param("dimension") String dimension,
                                                 @Param("bizType") String bizType,
                                                 @Param("sinceDate") LocalDate sinceDate,
                                                 @Param("minCount") int minCount,
                                                 @Param("limit") int limit);
}
