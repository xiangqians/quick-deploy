package org.xiangqian.quick.deploy.util;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @author xiangqian
 * @date 2026/04/03 10:44
 */
public class ThreadTest {

    private final static int port = 8080;

    @Test
    public void server() throws Exception {
        ServerSocket server = new ServerSocket(port);
        System.out.println("服务器已启动，监听 " + port + " 端口");
        while (true) {
            Socket client = server.accept();
            System.out.println(client.getRemoteSocketAddress() + " 客户端已连接");
            // 不发送任何数据，让客户端一直阻塞
        }
    }

    @Test
    public void test() throws Exception {
        Task task = new CmdTask();
        Thread thread = new Thread(task);
        thread.start();

        TimeUnit.SECONDS.sleep(10);
        System.out.println("开始中断线程 ...");
        task.close();
//        thread.stop();
        System.out.println("已中断线程，isAlive=" + thread.isAlive());
    }

    private static class CmdTask implements Task {
        private Closeable closeable;

        @SneakyThrows
        @Override
        public void close() throws IOException {
            CmdUtil.exec("cd D:\\xiangqian\\my\\quick-deploy\\quick-deploy\\proj\\vue\\repo && npm install",
                    closeable -> this.closeable = closeable,
                    System.out::println);
        }

        @Override
        public void run() {
            IOUtils.closeQuietly(closeable);
        }
    }

    private static class SocketTask implements Task {
        private Socket socket;

        @SneakyThrows
        @Override
        public void run() {
            try {
                socket = new Socket("localhost", port);
                InputStream in = socket.getInputStream();

                System.out.println("开始读取数据 ...");

                // 这里会一直阻塞，因为服务端不发送任何数据
                int data = in.read();

                System.out.println("收到数据：" + data);
            } finally {
                IOUtils.closeQuietly(this);
            }
        }

        @Override
        public void close() throws IOException {
            IOUtils.closeQuietly(socket);
            socket = null;
        }
    }

    private static interface Task extends Runnable, Closeable {

    }

}
