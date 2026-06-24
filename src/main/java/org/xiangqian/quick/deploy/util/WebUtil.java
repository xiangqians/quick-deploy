package org.xiangqian.quick.deploy.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author xiangqian
 * @date 2026/06/24 22:15
 */
public class WebUtil {

    public static HttpSession getSession() {
        ServletRequestAttributes reqAttrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return reqAttrs.getRequest().getSession();
    }

}
