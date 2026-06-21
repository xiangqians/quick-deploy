package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author xiangqian
 * @date 2026/04/22 17:19
 */
public class PrepareStage extends RunningStage {
    @JsonIgnore
    public static final String CODE = "prepare";

    public PrepareStage() {
        super(CODE, "准备");
    }

}
