package org.xiangqian.quick.deploy.model;

/**
 * @author xiangqian
 * @date 2026/04/22 17:27
 */
public class PullStage extends RunningStage {
    public static final String CODE = "pull";

    public PullStage() {
        super(CODE, "拉取");
    }
}
