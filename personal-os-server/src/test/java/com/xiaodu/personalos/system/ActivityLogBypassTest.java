package com.xiaodu.personalos.system;

import com.xiaodu.personalos.system.entity.ActActivityLog;
import com.xiaodu.personalos.system.mapper.ActivityLogMapper;
import com.xiaodu.personalos.system.service.ActivityLogService;
import com.xiaodu.personalos.system.service.impl.ActivityLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 活动流旁路写入单测（mock Mapper，无 Spring 上下文、不依赖 DB）。
 *
 * <p>验证设计铁律：活动流任何失败（DB 异常 / 必填缺失）只 warn 不抛，
 * 绝不影响调用方主流程。</p>
 *
 * @author Kou
 */
class ActivityLogBypassTest {

    /** DB 抛异常 → log() 吞掉不外抛，返回 null。 */
    @Test
    void mapperFailureNeverPropagates() {
        ActivityLogMapper mapper = Mockito.mock(ActivityLogMapper.class);
        when(mapper.insert(any(ActActivityLog.class)))
                .thenThrow(new RuntimeException("simulated db failure"));
        ActivityLogService service = new ActivityLogServiceImpl(mapper);

        // 主流程调用点：断言不抛异常
        Long id = assertDoesNotThrow(() -> service.log(1L, "work", "task_done", "完成周报"));
        assertNull(id);
    }

    /** 必填字段缺失 → 跳过写入，返回 null，不抛。 */
    @Test
    void missingRequiredFieldsSkippedSilently() {
        ActivityLogMapper mapper = Mockito.mock(ActivityLogMapper.class);
        ActivityLogService service = new ActivityLogServiceImpl(mapper);

        assertNull(service.log(1L, "work", "task_done", " "));
        assertNull(service.log(null, "work", "task_done", "标题"));
        assertNull(service.log(null));
    }

    /** 正常写入：id 回填、activityDate 由 PeriodUtil 折算、insert 确被调用。 */
    @Test
    void validEntryInsertedWithDerivedActivityDate() {
        ActivityLogMapper mapper = Mockito.mock(ActivityLogMapper.class);
        when(mapper.insert(any(ActActivityLog.class))).thenAnswer(inv -> {
            // 模拟 MyBatis-Plus 主键回填
            inv.getArgument(0, ActActivityLog.class).setId(100L);
            return 1;
        });
        ActivityLogService service = new ActivityLogServiceImpl(mapper);

        ActActivityLog entry = new ActActivityLog();
        entry.setUserId(1L);
        entry.setDimension("work");
        entry.setBizType("task_done");
        entry.setTitle("完成周报");
        entry.setOccurredAt(java.time.LocalDateTime.of(2026, 10, 1, 15, 0));

        Long id = service.log(entry);
        assertNotNull(id);
        assertEquals(100L, id);
        // activityDate 与 occurredAt 同日（自然日口径）
        assertEquals(java.time.LocalDate.of(2026, 10, 1), entry.getActivityDate());
        Mockito.verify(mapper, Mockito.times(1)).insert(any(ActActivityLog.class));
    }
}
