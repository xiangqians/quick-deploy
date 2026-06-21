package org.xiangqian.quick.deploy.util;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * SSH
 * <p>
 * JSch
 * http://www.jcraft.com/jsch
 * http://www.jcraft.com/jsch/examples
 * https://github.com/is/jsch
 * <p>
 * 第三方 JSch（第三方维护的版本）
 * https://github.com/mwiede/jsch
 *
 * @author xiangqian
 * @date 2022/07/23 13:10
 */
public class Ssh implements Closeable {

    protected Session session;

    /**
     * 构造函数
     *
     * @param host        服务器地址
     * @param port        服务器端口
     * @param user        用户
     * @param passwd      密码
     * @param connTimeout 连接超时时间
     * @throws Exception
     */
    public Ssh(String host, int port, String user, String passwd, Duration connTimeout) throws Exception {
        JSch jsch = new JSch();

        // 支持服务器身份验证，设置 known_host 文件位置
//        jsch.setKnownHosts();

        // public key authentication
//        jsch.addIdentity("location to private key file");

        // 设置服务器地址、端口、用户名、密码
        session = jsch.getSession(user, host, port);
        // password authentication
        session.setPassword(passwd);
        // 跳过公钥检测
        session.setConfig("StrictHostKeyChecking", "no");

        // 连接到服务器
        session.connect((int) connTimeout.toMillis());
    }

    /**
     * 打开通道
     *
     * @param type JSch 通道类型
     * @return
     * @throws Exception
     */
    protected Channel openChannel(Type type) throws Exception {
        return session.openChannel(type.getValue());
    }

    // JSch 通道类型
    protected static enum Type {
        SHELL("shell"),
        EXEC("exec"),
        SFTP("sftp"),
        ;
        @Getter
        private final String value;

        Type(String value) {
            this.value = value;
        }
    }

    /**
     * 执行命令
     *
     * @param cmd         命令
     * @param connTimeout 连接超时时间
     * @param closer      流关闭器
     * @param callback    结果回调
     * @throws Exception
     */
    @SneakyThrows
    public void exec(String cmd, Duration connTimeout, Consumer<Closeable> closer, Consumer<String> callback) {
        // 创建资源组，用于统一管理所有需要关闭的资源
        CloseableImpl closeable = new CloseableImpl();
        try {
            // 打开一个执行单行命令通道
            ChannelExec channel = (ChannelExec) openChannel(Type.EXEC);
            closeable.addLast(channel);
            // 获取输入流
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            // 设置命令
            channel.setCommand(cmd);
            // 连接
            channel.connect((int) connTimeout.toMillis());
            // 注册关闭回调，将资源组交给调用方管理
            closer.accept(closeable);
            // 读取输入流
            IoUtil.read(new SequenceInputStream(in, err), StandardCharsets.UTF_8, callback);
        } finally {
            IOUtils.closeQuietly(closeable);
        }
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            try {
                session.disconnect();
            } finally {
                session = null;
            }
        }
    }

}
