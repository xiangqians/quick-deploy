package org.xiangqian.quick.deploy.util;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * @author xiangqian
 * @date 2026/01/20 10:20
 */
public class SftpTest {

    private Sftp sftp;

    @Before
    public void before() throws Exception {
        String host = System.getenv("HOST");
        int port = Integer.parseInt(System.getenv("PORT"));
        String user = System.getenv("USER");
        String passwd = System.getenv("PASSWD");
        sftp = new Sftp(host, port, user, passwd, Duration.ofSeconds(60));
    }

    @After
    public void after() throws Exception {
        IOUtils.closeQuietly(sftp);
    }

    @Test
    public void test() throws Exception {
        // cd
        sftp.cd("tmp");

        // ls
        List<Sftp.FileEntry> fileEntries = sftp.ls("./");
        fileEntries.stream().forEach(System.out::println);

//        sftp.mkdir("test");
//        sftp.rm("test");
    }


    public void put() throws Exception {
        String src = "C:\\Users\\Administrator\\Downloads\\MinIO.png";
        sftp.put(src, Path.of(src).getFileName().toString(), System.out::println);
    }

    public void get() throws Exception {
        OutputStream out = null;
        try {
            String name = "MinIO.png";
            String dst = "D:\\xiangqian\\my\\project\\quick-deploy\\target\\" + name;
            sftp.get(name, dst, System.out::println);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

}
