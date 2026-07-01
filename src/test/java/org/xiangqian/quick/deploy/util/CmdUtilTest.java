package org.xiangqian.quick.deploy.util;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author xiangqian
 * @date 2026/01/21 17:01
 */
public class CmdUtilTest {

    @Test
    public void rmdir1() {
        CmdUtil.exec("rmdir /s /q D:\\xiangqian\\my\\project\\quick-deploy\\proj\\error", closeable -> {}, System.out::println);
    }

    @Test
    public void rmdir2() throws Exception {
        CmdUtil.exec(String.format("cd \"%s\" && if not exist %s rmdir /s /q %s",
                        "D:\\xiangqian\\work\\project\\platform\\sample\\target",
                        "sample-1.10.0.jar",
                        "classes"),
                closeable -> {},
                System.out::println);
    }

    @Test
    public void path() {
        String path = System.getenv("PATH");
        Arrays.asList(path.split(";")).forEach(value -> {
            if (value.startsWith("D:\\xiangqian\\")) {
                System.out.println(value);
            }
        });
    }

    @Test
    public void mvn1() throws Exception {
        CmdUtil.exec("cd D:\\xiangqian\\my\\project\\quick-deploy\\tmp\\proj\\dev\\platform\\repo && mvn-java11.cmd clean package",
                closeable -> {},
                System.out::println);
    }

    @Test
    public void mvn2() throws Exception {
        CmdUtil.exec(new File("D:\\xiangqian\\my\\project\\quick-deploy\\tmp\\proj\\dev\\platform\\repo"),
//                "mvn-java11.cmd clean"
//                "mvn-java11.cmd clean package"
                "mvn-java11.cmd clean && mvn-java11.cmd package"
                ,
                closeable -> {},
                System.out::println);
    }

    @Test
    public void npm() throws Exception {
        CmdUtil.exec("cd D:\\xiangqian\\my\\project\\quick-deploy\\proj\\frontend\\repo && npm install",
                closeable -> {},
                System.out::println);
    }

    @Test
    public void git() throws Exception {
        CmdUtil.exec("git", closeable -> {}, System.out::println);
    }


}
