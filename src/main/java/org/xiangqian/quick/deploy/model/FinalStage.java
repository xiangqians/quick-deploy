package org.xiangqian.quick.deploy.model;

/**
 * @author xiangqian
 * @date 2026/04/22 17:34
 */
public abstract class FinalStage extends Stage {

    public FinalStage(String code, String name) {
        super(code, name);
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public String getText() {
        return name;
    }

}