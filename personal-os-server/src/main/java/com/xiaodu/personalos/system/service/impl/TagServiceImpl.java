package com.xiaodu.personalos.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaodu.personalos.common.exception.BizException;
import com.xiaodu.personalos.system.dto.TagCreateDTO;
import com.xiaodu.personalos.system.entity.SysTag;
import com.xiaodu.personalos.system.entity.SysTagRel;
import com.xiaodu.personalos.system.mapper.TagMapper;
import com.xiaodu.personalos.system.mapper.TagRelMapper;
import com.xiaodu.personalos.system.result.SystemErrorCode;
import com.xiaodu.personalos.system.service.TagService;
import com.xiaodu.personalos.system.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 标签引擎服务实现。
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final TagRelMapper tagRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO create(TagCreateDTO dto) {
        String scope = StringUtils.hasText(dto.getScope()) ? dto.getScope() : "common";

        Long exists = tagMapper.selectCount(Wrappers.<SysTag>lambdaQuery()
                .eq(SysTag::getName, dto.getName())
                .eq(SysTag::getScope, scope));
        if (exists != null && exists > 0) {
            throw SystemErrorCode.TAG_NAME_EXISTS.toException();
        }

        SysTag tag = new SysTag();
        tag.setName(dto.getName());
        tag.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : "#639922");
        tag.setScope(scope);
        tag.setUseCount(0);
        tagMapper.insert(tag);

        log.info("新建标签: id={}, name={}, scope={}", tag.getId(), tag.getName(), tag.getScope());
        return toVO(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> list(String scope, String keyword) {
        List<SysTag> tags = tagMapper.selectList(Wrappers.<SysTag>lambdaQuery()
                .eq(StringUtils.hasText(scope), SysTag::getScope, scope)
                .like(StringUtils.hasText(keyword), SysTag::getName, keyword)
                .orderByDesc(SysTag::getUseCount)
                .orderByAsc(SysTag::getId));
        return tags.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bind(Long tagId, String bizType, Long bizId) {
        SysTag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw SystemErrorCode.TAG_NOT_FOUND.toException();
        }

        // 幂等：已绑定直接返回既有关联 id，不重复计数
        SysTagRel existing = tagRelMapper.selectOne(Wrappers.<SysTagRel>lambdaQuery()
                .eq(SysTagRel::getTagId, tagId)
                .eq(SysTagRel::getBizType, bizType)
                .eq(SysTagRel::getBizId, bizId));
        if (existing != null) {
            return existing.getId();
        }

        SysTagRel rel = new SysTagRel();
        rel.setTagId(tagId);
        rel.setBizType(bizType);
        rel.setBizId(bizId);
        tagRelMapper.insert(rel);

        // use_count + 1
        tagMapper.update(null, Wrappers.<SysTag>lambdaUpdate()
                .eq(SysTag::getId, tagId)
                .setSql("use_count = use_count + 1"));
        log.info("标签绑定: tagId={}, bizType={}, bizId={}, relId={}", tagId, bizType, bizId, rel.getId());
        return rel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long tagId, String bizType, Long bizId) {
        SysTagRel existing = tagRelMapper.selectOne(Wrappers.<SysTagRel>lambdaQuery()
                .eq(SysTagRel::getTagId, tagId)
                .eq(SysTagRel::getBizType, bizType)
                .eq(SysTagRel::getBizId, bizId));
        if (existing == null) {
            // 幂等：不存在即视为解绑成功
            return;
        }
        tagRelMapper.deleteById(existing.getId());

        // use_count 回减，地板 0
        tagMapper.update(null, Wrappers.<SysTag>lambdaUpdate()
                .eq(SysTag::getId, tagId)
                .gt(SysTag::getUseCount, 0)
                .setSql("use_count = use_count - 1"));
        log.info("标签解绑: tagId={}, bizType={}, bizId={}", tagId, bizType, bizId);
    }

    /** Entity → VO。 */
    private TagVO toVO(SysTag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setColor(tag.getColor());
        vo.setScope(tag.getScope());
        vo.setUseCount(tag.getUseCount());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }
}
