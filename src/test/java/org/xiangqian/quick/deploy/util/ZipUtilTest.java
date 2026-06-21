package org.xiangqian.quick.deploy.util;

import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author xiangqian
 * @date 2026/01/22 09:38
 */
public class ZipUtilTest {

    @Test
    public void compress() throws Exception {
        ZipUtil.compress("null",
                List.of(new File("C:\\Users\\Administrator\\Downloads\\test")),
                new File("C:\\Users\\Administrator\\Downloads\\test.zip"));
    }

    @Test
    public void decompress() throws Exception {
        ZipUtil.decompress(new File("C:\\Users\\Administrator\\Downloads\\test.zip"),
                new File("C:\\Users\\Administrator\\Downloads"));
    }

}
