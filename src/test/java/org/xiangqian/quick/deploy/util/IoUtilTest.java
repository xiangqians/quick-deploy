package org.xiangqian.quick.deploy.util;

import org.junit.Test;

/**
 * @author xiangqian
 * @date 2026/02/04 10:48
 */
public class IoUtilTest {

    @Test
    public void test() {
        System.out.println(IoUtil.humanBytes(123L));
        System.out.println(IoUtil.humanBytes(102 * 123L));
        System.out.println(IoUtil.humanBytes(1024 * 102 * 123L));
        System.out.println(IoUtil.humanBytes(1024 * 1024 * 102 * 12L));
    }

}
