package org.xiangqian.quick.deploy.util;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author xiangqian
 * @date 11:49 2024/03/09
 */
public class CmdUtil {

    /**
     * 执行命令
     *
     * @param cmd      命令
     * @param closer   流关闭器
     * @param callback 结果回调
     */
    @SneakyThrows
    public static void exec(String cmd, Consumer<Closeable> closer, Consumer<String> callback) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // Windows NT: ?
            // Windows 95: ?
            // Windows 10: cmd /c
            cmd = "cmd /c " + cmd;
        } else if (SystemUtils.IS_OS_LINUX) {
            cmd = "/bin/sh -c " + cmd;
        } else {
            throw new UnsupportedOperationException(String.format("当前操作系统（NAME=%s，ARCH=%s，VERSION=%s）不支持命令执行，仅支持 Windows 和 Linux",
                    SystemUtils.OS_NAME,
                    SystemUtils.OS_ARCH,
                    SystemUtils.OS_VERSION));
        }

        // 创建资源组，用于统一管理所有需要关闭的资源
        CloseableImpl closeable = new CloseableImpl();
        try {
            // 执行命令
            Process process = Runtime.getRuntime().exec(cmd);
            closeable.addLast(process);

            // 合并标准输入流和错误流，并用 UTF-8 编码读取
            BufferedReader reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(process.getInputStream(), process.getErrorStream()), StandardCharsets.UTF_8));
            closeable.addFirst(reader);

            // 注册关闭回调，将资源组交给调用方管理
            closer.accept(closeable);

            // 逐行读取命令执行结果，通过回调函数返回
            String line = null;
            while ((line = reader.readLine()) != null) {
                callback.accept(line);
            }

            // 等待外部进程处理完成，并获取外部进程的返回值
            int exitValue = process.waitFor();
        } finally {
            IOUtils.closeQuietly(closeable);
        }
    }

}
