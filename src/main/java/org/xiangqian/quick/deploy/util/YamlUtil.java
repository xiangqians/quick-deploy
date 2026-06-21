package org.xiangqian.quick.deploy.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;

import java.io.File;

/**
 * @author xiangqian
 * @date 2026/04/15 15:58
 */
public class YamlUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    static {
        // 属性为 NULL 不序列化
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 忽略未知属性 —— 当禁用反序列化时遇到未知属性报错时，Jackson 默认情况下要求 JSON 字符串中的所有属性都要与 Java 类的字段完全匹配，如果 JSON 中包含了未知属性，就会抛出异常
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SneakyThrows
    public static <T> T deser(File file, TypeReference<T> typeRef) {
        return MAPPER.readValue(file, typeRef);
    }

}
