package org.xiangqian.quick.deploy.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 哈希值计算工具类
 * MD5、SHA-1、SHA-256、SHA-512
 *
 * @author xiangqian
 * @date 2023/11/14 20:36
 */
public class HashUtil {

    public static String md5(byte[] data) {
        return DigestUtils.md5Hex(data);
    }

    public static String md5(String data) {
        return DigestUtils.md5Hex(data);
    }

}
