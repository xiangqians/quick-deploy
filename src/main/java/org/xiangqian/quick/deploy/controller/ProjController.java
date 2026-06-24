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
    public ModelAndView index(ModelAndView modelAndView, @RequestParam(name = "groupId", required = false) String groupId) {
        projService.list(groupId).forEach(modelAndView::addObject);
        modelAndView.setViewName("index");
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping("/proj/{groupId}/{projId}/{commitId}/prevCommits")
    public List<Git.Commit> prevCommits(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId, @PathVariable("commitId") String commitId) {
        return projService.prevCommits(groupId, projId, commitId);
    }

    @RequestMapping("/proj/{groupId}/{projId}/pull")
    public RedirectView pull(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId) {
        projService.pull(groupId, projId);
        return redirectView("/");
    }

    @RequestMapping("/proj/{groupId}/{projId}/{commitId}/deploy")
    public RedirectView deploy(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId, @PathVariable("commitId") String commitId) {
        projService.deploy(groupId, projId, commitId, proj -> true);
        return redirectView("/");
    }

    @ResponseBody
    @RequestMapping("/proj/{groupId}/{projId}/deploy/webhook")
    public Boolean webhookDeploy(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId, @RequestParam(name = "token", required = false) String token) {
        SecurityUtil.setWebhookUser();
        return projService.deploy(groupId, projId, "HEAD", proj -> StringUtils.equals(proj.getToken(), token));
    }

    @RequestMapping("/proj/{groupId}/{projId}/resume")
    public RedirectView resume(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId) {
        projService.resume(groupId, projId);
        return redirectView("/");
    }

    @RequestMapping("/proj/{groupId}/{projId}/abort")
    public RedirectView abort(@PathVariable("groupId") String groupId, @PathVariable("projId") String projId) {
        projService.abort(groupId, projId);
        return redirectView("/");
    }

    @RequestMapping("/proj/{groupId}/{projId}/record/list")
    public ModelAndView recordList(ModelAndView modelAndView, @PathVariable("groupId") String groupId, @PathVariable("projId") String projId) {
        projService.recordList(groupId, projId).forEach(modelAndView::addObject);
        modelAndView.setViewName("record/list");
        return modelAndView;
    }

    @RequestMapping("/proj/{groupId}/{projId}/record/{recordId}/log")
    public ModelAndView recordLog(ModelAndView modelAndView, @PathVariable("groupId") String groupId, @PathVariable("projId") String projId, @PathVariable("recordId") String recordId) {
        projService.recordLog(groupId, projId, recordId).forEach(modelAndView::addObject);
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
    @GetMapping(value = "/proj/{groupId}/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter event() {
        return projService.event();
    }

}
