package org.xiangqian.quick.deploy.model;

/**
 * @author xiangqian
 * @date 2026/04/30 10:09
 */
public interface Outer {
    void out(String content);

    default void outLn(String content) {
        out(content + '\n');
    }
}
