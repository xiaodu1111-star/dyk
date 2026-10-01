package com.xiaodu.personalos.system.service.impl;

import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.system.entity.ActActivityLog;
import com.xiaodu.personalos.system.mapper.ActivityLogMapper;
import com.xiaodu.personalos.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 活动流引擎服务实现（旁路写入，不暴露 REST）。
 *
 * <p><b>设计铁律</b>：本服务永远不向调用方抛异常——活动流只是旁路记录，
 * 失败只记 warn。主业务事务绝不因活动流而回滚。</p>
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogMapper activityLogMapper;

    @Override
    public Long log(ActActivityLog entry) {
        // 入口即吞：任何异常（含参数缺失、DB 故障）都不外抛
        try {
            if (entry == null) {
                log.warn("[活动流旁路] 入参为空，跳过写入");
                return null;
            }
            if (entry.getUserId() == null
                    || !StringUtils.hasText(entry.getDimension())
                    || !StringUtils.hasText(entry.getBizType())
                    || !StringUtils.hasText(entry.getTitle())) {
                log.warn("[活动流旁路] 必填字段缺失(userId/dimension/bizType/title)，跳过写入: {}", entry.getTitle());
                return null;
            }

            // occurredAt 为空取当前时刻；activityDate 统一走 PeriodUtil 折算
            if (entry.getOccurredAt() == null) {
                entry.setOccurredAt(LocalDateTime.now());
            }
            entry.setActivityDate(PeriodUtil.effectiveDate(entry.getOccurredAt()));

            activityLogMapper.insert(entry);
            return entry.getId();
        } catch (Exception e) {
            log.warn("[活动流旁路] 写入失败，不影响主流程: title={}, err={}",
                    entry == null ? null : entry.getTitle(), e.getMessage());
            return null;
        }
    }

    @Override
    public Long log(Long userId, String dimension, String bizType, String title) {
        ActActivityLog entry = new ActActivityLog();
        entry.setUserId(userId);
        entry.setDimension(dimension);
        entry.setBizType(bizType);
        entry.setTitle(title);
        return log(entry);
    }
}
