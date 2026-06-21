package org.xiangqian.quick.deploy.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;
import org.xiangqian.quick.deploy.model.Proj;
import org.xiangqian.quick.deploy.service.ProjService;
import org.xiangqian.quick.deploy.util.Git;
import org.xiangqian.quick.deploy.util.SecurityUtil;

import java.util.Collections;
import java.util.List;

/**
 * @author xiangqian
 * @date 2026/01/19 11:16
 */
@Controller
public class ProjController extends AbsController {

    @Autowired
    private ProjService projService;

    @RequestMapping("/")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.addObject("projs", projService.list());

        // test
        List<Proj> projs = (List<Proj>) modelAndView.getModel().get("projs");
        Proj testProj = null;
        for (Proj proj : projs) {
            if ("test".equals(proj.getId())) {
                testProj = proj;
                break;
            }
        }
        if (testProj != null) {
            projs.addAll(Collections.nCopies(20, testProj));
        }

        modelAndView.setViewName("index");
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping("/proj/{id}/{commitId}/prevCommits")
    public List<Git.Commit> prevCommits(@PathVariable("id") String id, @PathVariable("commitId") String commitId) {
        return projService.prevCommits(id, commitId);
    }

    @RequestMapping("/proj/{id}/pull")
    public RedirectView pull(@PathVariable("id") String id) {
        projService.pull(id);
        return redirectView("/");
    }

    @RequestMapping("/proj/{id}/{commitId}/deploy")
    public RedirectView deploy(@PathVariable("id") String id, @PathVariable("commitId") String commitId) {
        projService.deploy(id, commitId, proj -> true);
        return redirectView("/");
    }

    @ResponseBody
    @RequestMapping("/proj/{id}/deploy/webhook")
    public Boolean webhookDeploy(@PathVariable("id") String id, @RequestParam(name = "token", required = false) String token) {
        SecurityUtil.setWebhookUser();
        return projService.deploy(id, "HEAD", proj -> StringUtils.equals(proj.getToken(), token));
    }

    @RequestMapping("/proj/{id}/resume")
    public RedirectView resume(@PathVariable("id") String id) {
        projService.resume(id);
        return redirectView("/");
    }

    @RequestMapping("/proj/{id}/abort")
    public RedirectView abort(@PathVariable("id") String id) {
        projService.abort(id);
        return redirectView("/");
    }

    @RequestMapping("/proj/{projId}/record/list")
    public ModelAndView recordList(ModelAndView modelAndView, @PathVariable("projId") String projId) {
        projService.recordList(projId).forEach(modelAndView::addObject);
        modelAndView.setViewName("record/list");
        return modelAndView;
    }

    @RequestMapping("/proj/{projId}/record/{recordId}/log")
    public ModelAndView recordLog(ModelAndView modelAndView, @PathVariable("projId") String projId, @PathVariable("recordId") String recordId) {
        projService.recordLog(projId, recordId).forEach(modelAndView::addObject);
        modelAndView.setViewName("record/log");
        return modelAndView;
    }

    // 什么是 SSE？
    // SSE（Server-Sent Events）是一种服务器向客户端推送实时数据的技术，基于 HTTP 协议，使用简单的文本格式传输数据。
    //
    // SSE 特点：
    // 1）单向通信：只能服务器向客户端推送
    // 2）基于 HTTP：无需特殊协议
    // 3）自动重连：浏览器自动处理连接断开
    // 4）轻量级：相比 WebSocket 更简单
    @GetMapping(value = "/proj/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter event() {
        return projService.event();
    }

}
