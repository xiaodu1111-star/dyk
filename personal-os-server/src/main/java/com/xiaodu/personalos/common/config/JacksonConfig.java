package com.xiaodu.personalos.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置。
 *
 * <p>统一 Java 时间类型的传输格式（对齐《backend-architecture.md》7.6 节）：
 * {@code LocalDateTime -> yyyy-MM-dd HH:mm:ss}，{@code LocalDate -> yyyy-MM-dd}。
 * 时区与日期格式另见 {@code application.yml} 的 {@code spring.jackson}。</p>
 *
 * <p>说明：Long/BigDecimal 精度保护（转字符串）留待到出现雪花 ID / 高精度金额时再开启，
 * 本期 id 为自增 INT，保持数值类型以便与前端契约一致。</p>
 *
 * @author Kou
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializers(new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.serializers(new LocalDateSerializer(dateFormatter));
            builder.deserializers(new LocalDateDeserializer(dateFormatter));
        };
    }
}
