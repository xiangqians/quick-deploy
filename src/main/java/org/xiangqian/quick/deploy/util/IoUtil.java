package org.xiangqian.quick.deploy.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author xiangqian
 * @date 2026/01/30 15:42
 */
public class IoUtil {

    /**
     * 读取输入流
     *
     * @param inputStream 输入流
     * @param charset     字符集
     * @param callback    结果回调
     */
    public static void read(InputStream inputStream, Charset charset, Consumer<String> callback) {
        BufferedReader reader = null;
        try {
            String line = null;
            reader = new BufferedReader(new InputStreamReader(inputStream, charset));
            while ((line = reader.readLine()) != null) {
                callback.accept(line);
            }
        } catch (Exception e) {
            try (PrintWriter writer = newPrintWriter(callback)) {
                e.printStackTrace(writer);
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * 创建一个打印写入器
     *
     * @param callback 结果回调
     * @return
     */
    public static PrintWriter newPrintWriter(Consumer<String> callback) {
        return new PrintWriter(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new UnsupportedEncodingException();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                callback.accept(new String(b, off, len, StandardCharsets.UTF_8));
            }
        });
    }


    /**
     * 人性化字节
     * <p>
     * 1B(Byte) = 8b(bit)
     * 1KB = 1024B
     * 1MB = 1024KB
     * 1GB = 1024MB
     * 1TB = 1024GB
     *
     * @param b Byte
     * @return
     */
    public static String humanBytes(long b) {
        if (b < 0) {
            return "-";
        }

        if (b == 0) {
            return "0B";
        }

        StringBuilder builder = new StringBuilder();

        // GB
        long gb = b / (1024 * 1024 * 1024);
        if (gb > 0) {
            builder.append(gb).append("GB");
            b = b % (1024 * 1024 * 1024);
        }

        // MB
        long mb = b / (1024 * 1024);
        if (mb > 0) {
            boolean isEmpty = builder.isEmpty();
            builder.append(mb).append("MB");
            if (!isEmpty) {
                return builder.toString();
            }
            b = b % (1024 * 1024);
        }

        // KB
        long kb = b / 1024;
        if (kb > 0) {
            boolean isEmpty = builder.isEmpty();
            builder.append(kb).append("KB");
            if (!isEmpty) {
                return builder.toString();
            }
            b = b % 1024;
        }

        // B
        if (b > 0) {
            builder.append(b).append("B");
        }
        return builder.toString();
    }

}
