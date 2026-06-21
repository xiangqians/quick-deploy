package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 命令
 *
 * @author xiangqian
 * @date 2026/04/15 14:11
 */
@Data
public class Cmd {
    // 名称
    private String name;
    // 执行的命令
    private String run;
    // 满足条件时暂停，需用户确认后执行
    private String pause;
    // 索引
    @JsonIgnore
    private Integer index;
}
