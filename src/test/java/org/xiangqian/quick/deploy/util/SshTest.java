package org.xiangqian.quick.deploy.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 2026/01/19 18:48
 */
public class SshTest {

    private Ssh ssh;

    @Before
    public void before() throws Exception {
        String host = System.getenv("HOST");
        int port = Integer.parseInt(System.getenv("PORT"));
        String user = System.getenv("USER");
        String passwd = System.getenv("PASSWD");
        ssh = new Ssh(host, port, user, passwd, Duration.ofSeconds(60));
    }

    @After
    public void after() {
        IOUtils.closeQuietly(ssh);
    }

    private void exec(String cmd) throws Exception {
        System.out.println("$ " + cmd);
        ssh.exec(cmd, Duration.ofSeconds(60), closeable -> {}, System.out::println);
    }

    @Test
    public void test() throws Exception {
        exec("ls -l");
        System.out.println();

        exec("ll");

//        exec(String.format("cd %s && unzip %s", "/root/quick-deploy/a83fa3cfe868addf372e42e6df04b238", "tmp.zip"));
    }

}
