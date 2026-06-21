package org.xiangqian.quick.deploy.util;

import org.apache.commons.collections4.MapUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * @author xiangqian
 * @date 2026/04/16 16:05
 */
public class SpelUtil {

    public static <T> T evaluate(String expr, Map<String, Object> variables, Class<T> type) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (MapUtils.isNotEmpty(variables)) {
            variables.forEach(context::setVariable);
        }
        return parser.parseExpression(expr).getValue(context, type);
    }

}
