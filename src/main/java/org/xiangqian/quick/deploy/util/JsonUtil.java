package org.xiangqian.quick.deploy.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 工具类
 *
 * @author xiangqian
 * @date 2020/11/09 21:02
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 属性为 NULL 不序列化
        // 默认：JsonInclude.Include.ALWAYS
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 启用允许 null 键的特性
//        MAPPER.enable(SerializationFeature.WRITE_NULL_MAP_VALUES);

        // 忽略未知属性 —— 当禁用反序列化时遇到未知属性报错时，Jackson 默认情况下要求 JSON 字符串中的所有属性都要与 Java 类的字段完全匹配，如果 JSON 中包含了未知属性，就会抛出异常
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 查找和注册模块
//        MAPPER.findAndRegisterModules();

        // 禁用将时间写为时间戳
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 禁用将持续时间写为时间戳
        MAPPER.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        // 设置 java.util.Date 格式
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
        // 注册模块
        JavaTimeModule module = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        module.addSerializer(LocalDate.class, new LocalDateSerializer(formatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(formatter));
        formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(formatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(formatter));
        // ISO-8601 Duration 格式规则：
        // P - 开始标记
        // D - 天（日期部分）
        // T - 时间部分开始标记
        // H - 小时
        // M - 分钟
        // S - 秒
        // com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer 是 Jackson 库提供的标准反序列化器，它能够将类似 P1D、PT1H1M60S、P1DT1H1M60S 等格式的字符串转换为 java.time.Duration 对象。
        // 然而，60s 这种格式并不符合 ISO-8601 标准的 Duration 表示法，会解析会失败。
//        javaTimeModule.addDeserializer(Duration.class, new DurationDeserializer());
        // 自定义 java.time.Duration
        module.addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
            @Override
            public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
                String value = parser.getText().trim().toUpperCase();
                if (!value.startsWith("P")) {
                    int index = value.indexOf("D");
                    // 时分秒
                    if (index == -1) {
                        value = "PT" + value;
                    }
                    // 天
                    else if (index == value.length() - 1) {
                        value = "P" + value;
                    }
                    // 天+时分秒
                    else {
                        value = "P" + value.substring(0, index) + "DT" + value.substring(index + 1);
                    }
                }
                return Duration.parse(value);
            }
        });
        module.addSerializer(Duration.class, new JsonSerializer<Duration>() {
            @Override
            public void serialize(Duration value, JsonGenerator generator, SerializerProvider provider) throws IOException {
                String str = null;
                if (value == null) {
                    str = null;
                } else if (value.isZero()) {
                    str = "0s";
                } else {
                    StringBuilder builder = new StringBuilder();

                    // 天
                    long days = value.toDaysPart();
                    if (days > 0) {
                        builder.append(days).append("d");
                    }

                    // 小时
                    int hours = value.toHoursPart();
                    if (hours > 0) {
                        builder.append(hours).append("h");
                    }

                    // 分钟
                    int minutes = value.toMinutesPart();
                    if (minutes > 0) {
                        builder.append(minutes).append("m");
                    }

                    // 秒
                    int seconds = value.toSecondsPart();
                    if (seconds > 0) {
                        builder.append(seconds).append("s");
                    }

                    str = builder.toString();
                }
                generator.writeString(str);
            }
        });
        MAPPER.registerModule(module);
    }

    public static String serToStr(Object obj) {
        return serToStr(obj, false);
    }

    @SneakyThrows
    public static String serToStr(Object obj, boolean indent) {
        if (indent) {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }
        return MAPPER.writeValueAsString(obj);
    }

    @SneakyThrows
    public static byte[] serToBytes(Object obj) {
        return MAPPER.writeValueAsBytes(obj);
    }

    @SneakyThrows
    public static <T> T deser(String str, Class<T> type) {
        return MAPPER.readValue(str, type);
    }

    @SneakyThrows
    public static <T> T deser(String str, TypeReference<T> typeRef) {
        return MAPPER.readValue(str, typeRef);
    }

    @SneakyThrows
    public static <T> T deser(byte[] bytes, Class<T> type) {
        return MAPPER.readValue(bytes, type);
    }

    @SneakyThrows
    public static <T> T deser(byte[] bytes, TypeReference<T> typeRef) {
        return MAPPER.readValue(bytes, typeRef);
    }

    public static <T> T deser(Object obj, Class<T> type) {
        return MAPPER.convertValue(obj, type);
    }

    public static <T> T deser(Object obj, TypeReference<T> typeRef) {
        return MAPPER.convertValue(obj, typeRef);
    }

}
