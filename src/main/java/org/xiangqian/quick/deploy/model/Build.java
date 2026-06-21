package org.xiangqian.quick.deploy.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 构建
 *
 * @author xiangqian
 * @date 2026/01/26 16:25
 */
@Data
@ToString(callSuper = true)
public class Build extends AbsCmds {
    // 目标集
    private List<String> targets;
}
