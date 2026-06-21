package org.xiangqian.quick.deploy.model;

import lombok.Data;

/**
 * 服务器
 *
 * @author xiangqian
 * @date 2026/01/20 11:05
 */
@Data
public class Server {
    // 主机
    private String host;
    // 端口
    private Integer port;
    // 用户
    private String user;
    // 密码
    private String passwd;
}
