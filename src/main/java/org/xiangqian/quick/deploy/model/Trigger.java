package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Duration;

/**
 * 触发器
 *
 * @author xiangqian
 * @date 2026/07/01 21:29
 */
@Data
public class Trigger {
    private Webhook webhook;
    private Polling polling;

    // Webhook（网络钩子）触发：通过 HTTP 回调触发部署
    @Data
    public static class Webhook {
        // 认证令牌
        private String token;
    }

    // 轮询触发：定期检测并触发部署
    @Data
    public static class Polling {
        // 轮询间隔，单位：秒
        private Integer interval;

        @JsonIgnore
        private long lastTime;
    }
}
