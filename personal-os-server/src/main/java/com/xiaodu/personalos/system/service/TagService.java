package com.xiaodu.personalos.system.service;

import com.xiaodu.personalos.system.dto.TagCreateDTO;
import com.xiaodu.personalos.system.vo.TagVO;

import java.util.List;

/**
 * 标签引擎服务（M0 基础设施，表 sys_tag / sys_tag_rel）。
 *
 * <p>多态关联范式：业务 Service 先落自己的强语义表拿到 id，
 * 再调用 {@code bind / unbind} 维护标签关联，同一事务。</p>
 *
 * @author Kou
 */
public interface TagService {

    /**
     * 新建标签（name + scope 联合唯一）。
     *
     * @param dto 入参
     * @return 新建标签
     * @throws com.xiaodu.personalos.common.exception.BizException 15100 同名标签已存在
     */
    TagVO create(TagCreateDTO dto);

    /**
     * 查询标签列表。
     *
     * @param scope   所属域过滤，null/空 = 全部
     * @param keyword 名称模糊过滤，null/空 = 不过滤
     * @return 标签列表（use_count 降序，id 升序）
     */
    List<TagVO> list(String scope, String keyword);

    /**
     * 绑定标签到业务实体（幂等：关联已存在时直接返回既有关联 id，不重复计数）。
     *
     * @param tagId   标签 id
     * @param bizType 业务实体类型
     * @param bizId   业务实体 id
     * @return 关联记录 id
     * @throws com.xiaodu.personalos.common.exception.BizException 15101 标签不存在
     */
    Long bind(Long tagId, String bizType, Long bizId);

    /**
     * 解绑（幂等：关联不存在时静默返回）。同步回减 use_count。
     *
     * @param tagId   标签 id
     * @param bizType 业务实体类型
     * @param bizId   业务实体 id
     */
    void unbind(Long tagId, String bizType, Long bizId);
}
