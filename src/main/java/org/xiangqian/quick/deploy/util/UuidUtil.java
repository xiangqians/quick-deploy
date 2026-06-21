package org.xiangqian.quick.deploy.util;

import java.util.UUID;

/**
 * @author xiangqian
 * @date 2026/01/21 09:40
 */
public class UuidUtil {

    public static String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
