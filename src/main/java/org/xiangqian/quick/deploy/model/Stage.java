package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

/**
 * @author xiangqian
 * @date 2026/04/22 17:17
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,         // 使用名称标识
        include = JsonTypeInfo.As.PROPERTY, // 作为属性
        property = "code"                   // 类型字段名
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PrepareStage.class, name = PrepareStage.CODE),
        @JsonSubTypes.Type(value = PullStage.class, name = PullStage.CODE),
        @JsonSubTypes.Type(value = BuildStage.class, name = BuildStage.CODE),
        @JsonSubTypes.Type(value = DeployStage.class, name = DeployStage.CODE),
        @JsonSubTypes.Type(value = AbortStage.class, name = AbortStage.CODE),
        @JsonSubTypes.Type(value = SuccessStage.class, name = SuccessStage.CODE),
        @JsonSubTypes.Type(value = FailureStage.class, name = FailureStage.CODE),
})
public abstract class Stage {

    @Getter
    @JsonIgnore
    protected final String code;

    @Getter
    @JsonIgnore
    protected final String name;

    public Stage(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonIgnore
    public abstract String getLabel();

    @JsonIgnore
    public abstract String getText();

    @JsonIgnore
    public boolean isPrepare() {
        return PrepareStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isPull() {
        return PullStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isBuild() {
        return BuildStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isDeploy() {
        return DeployStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isAbort() {
        return AbortStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SuccessStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isFailure() {
        return FailureStage.CODE.equals(getCode());
    }

    @JsonIgnore
    public boolean isRunning() {
        return isPrepare() || isPull() || isBuild() || isDeploy();
    }

    @JsonIgnore
    public boolean isPaused() {
        return false;
    }

    @JsonIgnore
    public boolean isFinal() {
        return isAbort() || isSuccess() || isFailure();
    }

}
