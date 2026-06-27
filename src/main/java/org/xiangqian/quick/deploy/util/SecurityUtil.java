package org.xiangqian.quick.deploy.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.xiangqian.quick.deploy.model.User;

/**
 * @author xiangqian
 * @date 2024/03/02 15:26
 */
public class SecurityUtil {

    private static ThreadLocal<SecurityUser> threadLocal = new ThreadLocal<>();

    /**
     * 获取已通过身份验证的用户信息
     *
     * @return
     */
    public static SecurityUser getUser() {
        SecurityUser user = threadLocal.get();
        if (user != null) {
            return user;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 匿名用户（表示用户未登录）
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // 用户已通过身份验证（已登录）
        if (authentication != null && authentication.isAuthenticated()) {
            return (SecurityUser) authentication.getPrincipal();
        }

        return null;
    }

    @Deprecated
    private static void setUser(SecurityUser user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void setWebhookUser() {
        threadLocal.set(new SecurityUser(User.WEBHOOK));
    }

    public static void removeWebhookUser() {
        threadLocal.remove();
    }

}
