package org.xiangqian.quick.deploy.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author xiangqian
 * @date 2026/01/21 17:42
 */
public class ZipUtil {

    /**
     * 压缩
     *
     * @param folder
     * @param files  文件集
     * @param zip    ZIP 文件
     * @throws IOException
     */
    public static void compress(String folder, List<File> files, File zip) throws IOException {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zip));
            // 压缩
            for (File file : files) {
                compress(folder, file, out);
            }
            // 完成写入
            out.finish();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private static void compress(String folder, File file, ZipOutputStream out) throws IOException {
        // 文件夹
        if (file.isDirectory()) {
            if (folder == null) {
                folder = file.getName();
            } else {
                folder = folder + "/" + file.getName();
            }
            for (File subfile : file.listFiles()) {
                compress(folder, subfile, out);
            }
        }
        // 文件
        else {
            FileInputStream in = null;
            try {
                // 创建条目
                String name = file.getName();
                if (folder != null) {
                    name = folder + "/" + name;
                }
                out.putNextEntry(new ZipEntry(name));

                // 写入文件内容
                int len = 0;
                byte[] bytes = new byte[1024];
                in = new FileInputStream(file);
                while ((len = in.read(bytes)) > 0) {
                    out.write(bytes, 0, len);
                }

                // 关闭条目
                out.closeEntry();
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
     * 解压
     *
     * @param zip    ZIP 文件
     * @param folder 解压到指定文件夹
     * @throws IOException
     */
    public static void decompress(File zip, File folder) throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new FileInputStream(zip));
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                File newFile = new File(folder, entry.getName());
                if (entry.isDirectory()) {
                    if (!newFile.exists()) {
                        newFile.mkdirs();
                    }
                } else {
                    File parentFile = newFile.getParentFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }

                    FileOutputStream out = null;
                    try {
                        int len = 0;
                        byte[] bytes = new byte[1024];
                        out = new FileOutputStream(newFile);
                        while ((len = in.read(bytes)) > 0) {
                            out.write(bytes, 0, len);
                        }
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }
                entry = in.getNextEntry();
            }

            // 关闭条目
            in.closeEntry();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
