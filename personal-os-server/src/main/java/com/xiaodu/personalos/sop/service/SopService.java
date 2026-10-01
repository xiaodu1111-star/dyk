package com.xiaodu.personalos.sop.service;

import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.sop.dto.SopRunFinishDTO;
import com.xiaodu.personalos.sop.dto.SopSaveDTO;
import com.xiaodu.personalos.sop.vo.SopDetailVO;
import com.xiaodu.personalos.sop.vo.SopRunVO;
import com.xiaodu.personalos.sop.vo.SopVO;
import com.xiaodu.personalos.sop.vo.SopVersionDetailVO;

/**
 * SOP 库服务（M2）。
 *
 * <p>所有方法强制以「当前登录用户」为边界，查询恒拼 {@code user_id}。</p>
 *
 * @author Alex
 */
public interface SopService {

    /**
     * 创建 SOP（含可选步骤），返回新建 id。
     *
     * @param dto 创建入参
     * @return 新建 SOP id
     */
    Long create(SopSaveDTO dto);

    /**
     * 分页查询 SOP 列表。
     *
     * <p>排序：pinned desc → last_used_at desc（NULL 最后）→ use_count desc → id desc。</p>
     *
     * @param keyword  标题模糊关键词，可选
     * @param category 分类精确筛选，可选
     * @param page     页码，从 1 开始
     * @param size     每页条数
     * @return 分页结果
     */
    PageResult<SopVO> list(String keyword, String category, long page, long size);

    /**
     * 查询 SOP 详情（含步骤 / 版本摘要 / 执行统计）。
     *
     * @param id SOP id
     * @return 详情
     */
    SopDetailVO detail(Long id);

    /**
     * 编辑 SOP。
     *
     * <p>若该 SOP 已有执行记录 → 先写 work_sop_version 快照（旧内容），版本号 +0.1；
     * 步骤覆盖式全量替换。</p>
     *
     * @param id  SOP id
     * @param dto 编辑入参
     */
    void update(Long id, SopSaveDTO dto);

    /**
     * 逻辑删除 SOP，级联逻辑删步骤。
     *
     * @param id SOP id
     */
    void delete(Long id);

    /**
     * 回看版本完整快照（含 contentJson 解析内容）。
     *
     * @param sopId     SOP id
     * @param versionId 版本记录 id
     * @return 版本完整快照
     */
    SopVersionDetailVO versionDetail(Long sopId, Long versionId);

    /**
     * 发起一次执行（建 work_sop_log，started_at=now）。
     *
     * @param sopId SOP id
     * @return 执行记录 id（runId）
     */
    Long startRun(Long sopId);

    /**
     * 结束一次执行（同一事务完成回写与聚合）。
     *
     * @param runId 执行记录 id
     * @param dto   结束入参
     */
    void finishRun(Long runId, SopRunFinishDTO dto);

    /**
     * 分页查询执行历史（按 started_at 倒序）。
     *
     * @param sopId SOP id
     * @param page  页码
     * @param size  每页条数
     * @return 分页结果
     */
    PageResult<SopRunVO> runHistory(Long sopId, long page, long size);
}
