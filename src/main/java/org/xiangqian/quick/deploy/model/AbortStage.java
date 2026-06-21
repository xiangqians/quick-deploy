package org.xiangqian.quick.deploy.model;

/**
 * @author xiangqian
 * @date 2026/04/22 17:29
 */
public class AbortStage extends FinalStage {
    public static final String CODE = "abort";

    public AbortStage() {
        super(CODE, "中止");
    }

}
