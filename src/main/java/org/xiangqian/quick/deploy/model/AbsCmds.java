package org.xiangqian.quick.deploy.model;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author xiangqian
 * @date 2026/04/16 17:07
 */
@Data
public abstract class AbsCmds {
    // 命令集
    private List<Cmd> cmds;

    public void setCmds(List<Cmd> cmds) {
        if (CollectionUtils.isNotEmpty(cmds)) {
            for (int i = 0, size = cmds.size(); i < size; i++) {
                Cmd cmd = cmds.get(i);
                cmd.setIndex(i);
            }
        }
        this.cmds = cmds;
    }
}
