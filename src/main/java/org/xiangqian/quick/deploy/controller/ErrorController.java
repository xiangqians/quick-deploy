package org.xiangqian.quick.deploy.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * @author xiangqian
 * @date 2026/01/22 15:50
 */
@Controller
@RequestMapping("/error")
public class ErrorController extends AbsController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping
    public ModelAndView error(ModelAndView modelAndView, HttpServletRequest request) {
        modelAndView.addObject("code", request.getAttribute("jakarta.servlet.error.status_code"));

        String message = Optional.ofNullable(request.getAttribute("jakarta.servlet.error.message"))
                .map(Object::toString)
                .map(StringUtils::trim)
                .orElse("");
        Exception exception = Optional.ofNullable(request.getAttribute("jakarta.servlet.error.exception"))
                .filter(obj -> obj instanceof Exception)
                .map(obj -> (Exception) obj)
                .orElse(null);
        if (exception != null) {
            Throwable cause = exception.getCause();
            if (cause != null) {
                if (StringUtils.isNotEmpty(message)) {
                    message += '\n';
                }
                message += cause.getMessage();
            }
        }
        modelAndView.addObject("message", message);

        modelAndView.setViewName("error");
        return modelAndView;
    }

}
