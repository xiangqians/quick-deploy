package org.xiangqian.quick.deploy.util;

import com.jcraft.jsch.Channel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 可关闭的impl
 *
 * @author xiangqian
 * @date 2026/04/03 14:32
 */
public class CloseableImpl implements Closeable {

    private List<Closeable> list;

    public CloseableImpl() {
        this.list = new ArrayList<>();
    }

    public void addFirst(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                list.addFirst(closeable);
            }
        }
    }

    public void addLast(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                list.add(closeable);
            }
        }
    }

    public void addFirst(Process process) {
        if (process != null) {
            list.addFirst(process::destroyForcibly);
        }
    }

    public void addLast(Process process) {
        if (process != null) {
            list.add(process::destroyForcibly);
        }
    }

    public void addFirst(Channel channel) {
        if (channel != null) {
            list.addFirst(channel::disconnect);
        }
    }

    public void addLast(Channel channel) {
        if (channel != null) {
            list.add(channel::disconnect);
        }
    }

    @Override
    public void close() throws IOException {
        if (CollectionUtils.isNotEmpty(list)) {
            IOUtils.closeQuietly(list);
            list.clear();
        }
    }

}
