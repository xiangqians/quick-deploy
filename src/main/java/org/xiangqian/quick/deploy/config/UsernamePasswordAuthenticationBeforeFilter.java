package org.xiangqian.quick.deploy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.xiangqian.quick.deploy.util.SecurityUtil;

import java.io.IOException;

/**
 * @author xiangqian
 * @date 2026/01/26 14:58
 */
public class UsernamePasswordAuthenticationBeforeFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String servletPath = request.getServletPath();
        if (servletPath.matches("/proj/[^/]+/deploy/webhook")) {
            chain.doFilter(request, response);
            return;
        }

        if ("/login".equals(servletPath) && SecurityUtil.getUser() != null) {
            String ctxPath = request.getContextPath();
            response.sendRedirect(ctxPath + "/");
            return;
        }
        chain.doFilter(request, response);
    }

}