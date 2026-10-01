package com.xiaodu.personalos.life.service;

import com.xiaodu.personalos.life.dto.HabitCheckinDTO;
import com.xiaodu.personalos.life.dto.HabitCreateDTO;
import com.xiaodu.personalos.life.dto.QuickRecordDTO;
import com.xiaodu.personalos.life.dto.RecordConfirmDTO;
import com.xiaodu.personalos.life.vo.HabitVO;
import com.xiaodu.personalos.life.vo.QuickRecordVO;
import com.xiaodu.personalos.life.vo.RecordVO;
import com.xiaodu.personalos.life.vo.StreakVO;

import java.util.List;

/**
 * 生活域习惯服务（M3）。
 *
 * <p>习惯 = 指标引擎（{@code sys_metric_def}）的一条定义，打卡 = 一条记录
 * （{@code sys_metric_record}）。新增习惯不改表、不发版。
 * 本服务<b>不复用</b> {@code MetricService.record()}（其语义为同日 upsert），
 * 而是自行实现「打卡型幂等拒绝 / 计数型当日累加」的业务记录逻辑。</p>
 *
 * <p>时间口径一律走 {@code PeriodUtil}；周期边界不自算。</p>
 *
 * @author Kou
 */
public interface HabitService {

    /**
     * 新建习惯（写一条 dimension=life 的指标定义，code 自动生成且查重）。
     *
     * @param dto 入参（name 必填、type=checkin|count）
     * @return 新建的习惯（含今日状态）
     * @throws com.xiaodu.personalos.common.exception.BizException 14004 入参非法
     */
    HabitVO create(HabitCreateDTO dto);

    /**
     * 习惯列表（dimension=life，含今日完成状态）。
     *
     * @return 习惯列表（sort_no 升序、id 升序）
     */
    List<HabitVO> list();

    /**
     * 修改习惯（改名 / 目标 / 单位）。
     *
     * @param id  习惯 id
     * @param dto 入参
     * @return 修改后的习惯（含今日状态）
     * @throws com.xiaodu.personalos.common.exception.BizException 14001 习惯不存在
     */
    HabitVO update(Long id, HabitCreateDTO dto);

    /**
     * 删除习惯（逻辑删 def）。
     *
     * @param id 习惯 id
     * @throws com.xiaodu.personalos.common.exception.BizException 14001 习惯不存在
     */
    void delete(Long id);

    /**
     * 打卡。
     *
     * <p>打卡型：当日已存在记录 → 抛 14002（幂等拒绝）；否则插一条 value_num=1、value_text="1"。
     * 计数型：当日已有记录 → value_num += 增量；否则插一条 value_num=增量。
     * 成功后写活动流（life / checkin）。</p>
     *
     * @param id  习惯 id
     * @param dto 打卡入参（计数型增量，默认 1）
     * @return 落库后的记录
     * @throws com.xiaodu.personalos.common.exception.BizException 14001 习惯不存在；14002 今日已打卡；14004 入参非法
     */
    RecordVO checkin(Long id, HabitCheckinDTO dto);

    /**
     * 连续打卡天数（自然日口径）。
     *
     * <p>一次查询取最近 90 天记录后内存计算：今天有效则从今天算，
     * 否则从昨天算（今天还没打不算断），逐日向前直到遇无效日停止。</p>
     *
     * @param id 习惯 id
     * @return streak 结果
     * @throws com.xiaodu.personalos.common.exception.BizException 14001 习惯不存在
     */
    StreakVO streak(Long id);

    /**
     * 快捷记录解析（<b>只解析不落库</b>）。
     *
     * <p>格式 {@code <指标名前缀> [数字]}；匹配 dimension 为 life/common 中 name 以输入首词开头的第一条。
     * 解析失败抛 14003。</p>
     *
     * @param dto 入参（text）
     * @return 预填结果，供前端确认
     * @throws com.xiaodu.personalos.common.exception.BizException 14003 解析失败
     */
    QuickRecordVO quickRecord(QuickRecordDTO dto);

    /**
     * 确认落库（快捷记录与手动的统一入口）。
     *
     * <p>打卡型：与手动打卡一致（今日已存在 → 14002）；计数型：当日累加。</p>
     *
     * @param dto 入参（metricId 必填、value、date）
     * @return 落库后的记录
     * @throws com.xiaodu.personalos.common.exception.BizException 14001 习惯不存在；14002 今日已打卡；14004 入参非法
     */
    RecordVO confirmRecord(RecordConfirmDTO dto);
}
