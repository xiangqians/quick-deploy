package org.xiangqian.quick.deploy.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpProgressMonitor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * SFTP
 *
 * @author xiangqian
 * @date 2022/07/24 00:04
 */
public class Sftp extends Ssh {

    private ChannelSftp channel;

    public Sftp(String host, int port, String user, String passwd, Duration connTimeout) throws Exception {
        super(host, port, user, passwd, connTimeout);
        // 打开一个上传、下载文件通道
        channel = (ChannelSftp) openChannel(Type.SFTP);
        // 连接
        channel.connect((int) connTimeout.toMillis());
    }

    /**
     * 进入指定目录
     *
     * @param path
     * @throws Exception
     */
    public void cd(String path) throws Exception {
        channel.cd(path);
    }

    /**
     * 查询指定目录下的文件列表
     *
     * @param path
     * @return
     * @throws Exception
     */
    public List<FileEntry> ls(String path) throws Exception {
        Vector<?> vector = channel.ls(path);
        return Optional.ofNullable(vector)
                .map(list -> list.stream().map(Object::toString).map(FileEntry::parse).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * 创建目录
     *
     * @param path
     * @throws Exception
     */
    public void mkdir(String path) throws Exception {
        channel.mkdir(path);
    }

    /**
     * 删除普通文件、目录文件等等
     *
     * @param path
     * @throws Exception
     */
    public void rm(String path) throws Exception {
        try {
            // 删除普通文件
            channel.rm(path);
        } catch (Exception e) {
            // 删除目录文件（只允许删除空目录）
            channel.rmdir(path);
        }
    }

    /**
     * 上传文件
     *
     * @param src      本地源文件路径
     * @param dst      远程服务器上的目标文件路径
     * @param consumer
     * @throws Exception
     */
    @SneakyThrows
    public void put(String src, String dst, Consumer<String> consumer) {
        // 监控 SFTP 文件传输的进度
        SftpProgressMonitor monitor = new SftpProgressMonitorImpl(consumer);

        // 文件传输模式
        // ChannelSftp.OVERWRITE：完全覆盖模式，这是 JSch 的默认文件传输模式，即如果目标文件已经存在，传输的文件将完全覆盖目标文件，产生新的文件。
        // ChannelSftp.RESUME   ：恢复模式，如果文件已经传输一部分，这时由于网络或其他任何原因导致文件传输中断，如果下一次传输相同的文件，则会从上一次中断的地方续传。
        // ChannelSftp.APPEND   ：追加模式，如果目标文件已存在，传输的文件将在目标文件后追加。
        int mode = ChannelSftp.OVERWRITE;

        channel.put(src, dst, monitor, mode);
    }

    /**
     * 下载文件
     *
     * @param src      远程服务器上的源文件路径
     * @param dst      本地目标文件路径
     * @param consumer
     * @throws Exception
     */
    public void get(String src, String dst, Consumer<String> consumer) throws Exception {
        channel.get(src, dst, new SftpProgressMonitorImpl(consumer));
    }

    @Override
    public void close() throws IOException {
        try {
            if (channel != null) {
                channel.disconnect();
            }
        } finally {
            channel = null;
            super.close();
        }
    }

    /**
     * 监控 SFTP 文件传输的进度
     */
    public static class SftpProgressMonitorImpl implements SftpProgressMonitor {
        // 文件总大小
        private long max;
        // 已传输大小
        private long transferred;
        // 上一次时间
        private long prevTime;

        private Consumer<String> consumer;

        public SftpProgressMonitorImpl(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        /**
         * 初始化监控器
         *
         * @param op   操作类型：上传（{@link SftpProgressMonitor#PUT}） 或 下载（{@link SftpProgressMonitor#GET}）
         * @param src  源文件路径
         * @param dest 目标文件路径
         * @param max  文件总大小（字节），如果未知则为 {@link SftpProgressMonitor#UNKNOWN_SIZE}
         */
        @Override
        public void init(int op, String src, String dest, long max) {
            this.max = max;
            String opStr = switch (op) {
                case PUT -> "Upload";
                case GET -> "Download";
                default -> "Unknown";
            };
            consumer.accept(String.format("Starting %s: %s (%s) -> %s", opStr, src, IoUtil.humanBytes(max), dest));
        }

        /**
         * 进度更新回调
         *
         * @param count 当前已传输的字节数
         * @return 是否继续传输，true - 继续传输，false - 取消传输
         */
        @Override
        public boolean count(long count) {
            transferred += count;

            // 当前时间
            long currTime = System.currentTimeMillis();
            if (currTime - prevTime > 1 * 1000) {
                consumer.accept(String.format("Transferred: %s / %s (%.2f%%)", IoUtil.humanBytes(transferred), IoUtil.humanBytes(max), (transferred * 100.0 / max)));
                prevTime = currTime;
            }

            return true;
        }

        /**
         * 传输完成回调
         */
        @Override
        public void end() {
            consumer.accept("Transfer completed");
        }
    }

    /**
     * 文件条目
     * <p>
     * 示例：
     * drwxr-xr-x    2 root     root         4096 Jul  7 15:47 test
     * 解析：
     * type=d
     * mod=[rwx][r-x][r-x]
     * count=2
     * owner=root
     * group=root
     * size=4096
     * lastModifiedDate=Jul  7 15:47
     * name=test
     */
    @Data
    public static class FileEntry {

        // 文件类型
        // -：普通文件
        // d：目录文件
        // p：管理文件
        // l；链接文件
        // b：块设备文件
        // c：字符设备文件
        // s：套接字文件
        private String type;

        // mode，文件权限
        // r：读权限
        // w：写权限
        // x：可执行权限
        // -：无权限
        // [第1组]：拥有者权限
        // [第2组]：组用户权限
        // [第3组]：其他用户权限
        private String mod;

        // 对于普通文件：链接数
        // 对于目录文件：第一级子目录数
        private Integer count;

        // 拥有者
        private String owner;

        // 组
        private String group;

        // 文件大小
        private Long size;

        // 最后修改时间
        private String lastModifiedDate;

        // 文件名
        private String name;

        @Override
        public String toString() {
            return type + mod + '\t' + count + '\t' + owner + '\t' + group + '\t' + size + '\t' + lastModifiedDate + '\t' + name;
        }

        public static FileEntry parse(String text) {
            if (StringUtils.isEmpty(text = StringUtils.trim(text))) {
                return null;
            }

            String[] array = text.split("\\s+");
            if (array.length != 9) {
                return null;
            }

            FileEntry fileEntry = new FileEntry();
            fileEntry.setType(array[0].substring(0, 1));
            fileEntry.setMod(array[0].substring(1));
            fileEntry.setCount(NumberUtils.toInt(array[1], -1));
            fileEntry.setOwner(array[2]);
            fileEntry.setGroup(array[3]);
            fileEntry.setSize(NumberUtils.toLong(array[4]));
            fileEntry.setLastModifiedDate(StringUtils.join(array, " ", 5, 8));
            fileEntry.setName(array[8]);
            return fileEntry;
        }
    }

}
