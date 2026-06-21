package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author xiangqian
 * @date 2026/04/22 17:30
 */
public class FailureStage extends FinalStage {
    public static final String CODE = "failure";

    public FailureStage() {
        super(CODE, "失败");
    }

}
