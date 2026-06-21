package org.xiangqian.quick.deploy.model;

/**
 * @author xiangqian
 * @date 2026/04/22 17:28
 */
public class DeployStage extends RunningStage {
    public static final String CODE = "deploy";

    public DeployStage() {
        super(CODE, "部署");
    }

}
