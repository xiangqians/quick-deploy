package org.xiangqian.quick.deploy.util;

import org.junit.Test;

import java.util.Map;

/**
 * @author xiangqian
 * @date 2026/04/14 16:49
 */
public class SpelUtilTest {

    @Test
    public void test() {
        Map<String, Object> variables = Map.of("env", "test",
                "hour", 14,
                "user", Map.of("name", "Admin"));
        String expr = "#env == 'test'";
        System.out.println(SpelUtil.evaluate(expr, variables, Boolean.class));

        expr = "#env == 'test' and #hour < 3";
        System.out.println(SpelUtil.evaluate(expr, variables, Boolean.class));

        expr = "#user['name']";
        System.out.println(SpelUtil.evaluate(expr, variables, String.class));
    }

}
