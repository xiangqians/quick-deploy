package org.xiangqian.quick.deploy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.xiangqian.quick.deploy.service.UserService;
import org.xiangqian.quick.deploy.util.SecurityUser;
import org.xiangqian.quick.deploy.util.SecurityUtil;

import java.util.Optional;

/**
 * @author xiangqian
 * @date 2026/01/22 15:49
 */
public abstract class AbsController {

    @Autowired
    private UserService userService;

    private String ctxPath;

    @Value("${server.servlet.context-path}")
    public void setCtxPath(String ctxPath) {
        if ("/".equals(ctxPath)) {
            ctxPath = "";
        }
        this.ctxPath = ctxPath;
    }

    // 在每个请求之前设置ModelAndView值
    @ModelAttribute
    public void modelAttribute(ModelAndView modelAndView) {
        modelAndView.addObject("user", Optional.ofNullable(SecurityUtil.getUser()).map(SecurityUser::getUser).orElse(null));
        modelAndView.addObject("timestamp", System.currentTimeMillis());
    }

    protected RedirectView redirectView(String path) {
        return new RedirectView(ctxPath + path + "?t=" + System.currentTimeMillis());
    }

}
