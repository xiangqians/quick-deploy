package org.xiangqian.quick.deploy.model;

import lombok.Data;

/**
 * @author xiangqian
 * @date 2026/06/27 12:31
 */
@Data
public class Git {
    // 用户
    private String user;
    // 密码
    private String passwd;
    // 仓库
    private Repo repo;
}
