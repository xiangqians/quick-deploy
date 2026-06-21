package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.xiangqian.quick.deploy.util.DateTimeUtil;
import org.xiangqian.quick.deploy.util.Git;

import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 记录
 *
 * @author xiangqian
 * @date 2026/04/15 13:56
 */
@Data
public class Record {

    // 主键
    private String id;
    // 提交信息
    private Git.Commit commit;
    // 操作者
    private List<User> operators;
    // 阶段列表
    private List<Stage> stages;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private Object lock;

    @JsonIgnore
    private Path logFile;

    @JsonIgnore
    private String commitId;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private Closeable closeable;

    @JsonIgnore
    private Map<String, File> targets;

    @JsonIgnore
    private Thread thread;
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private boolean interrupted;
    @JsonIgnore
    private boolean aborted;

    @JsonIgnore
    public void addOperator(User operator) {
        synchronized (lock) {
            if (operators == null) {
                operators = new ArrayList<>(2);
            }
            if (!operators.contains(operator)) {
                operators.add(operator);
            }
        }
    }

    @JsonIgnore
    public User getFirstOperator() {
        synchronized (lock) {
            if (CollectionUtils.isNotEmpty(operators)) {
                return operators.getFirst();
            }
            return null;
        }
    }

    @JsonIgnore
    public List<String> getOperatorNicks() {
        synchronized (lock) {
            if (CollectionUtils.isNotEmpty(operators)) {
                return operators.stream().map(User::getNick).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }

    @JsonIgnore
    public String joinOperators(String separator) {
        synchronized (lock) {
            return StringUtils.join(operators, separator);
        }
    }

    @JsonIgnore
    public boolean pause(Cmd cmd) {
        synchronized (lock) {
            Stage stage = getLastStage();
            if (stage instanceof RunningStage rStage) {
                return rStage.pause(cmd);
            }
            return false;
        }
    }

    @JsonIgnore
    public boolean resume() {
        synchronized (lock) {
            Stage stage = getLastStage();
            if (isPaused(stage)) {
                RunningStage runningStage = (RunningStage) stage;
                RunningStage.Time time = runningStage.getLastTime();
                time.setStartTime(LocalDateTime.now());
                return true;
            }
            return false;
        }
    }

    @JsonIgnore
    public boolean abort() {
        synchronized (lock) {
            Stage stage = getLastStage();
            if (isPaused(stage)) {
                RunningStage runningStage = (RunningStage) stage;
                runningStage.removeLastTime();
                addAbortStage();
                return true;
            }
        }

        aborted = true;
        if (thread != null && !thread.isInterrupted()) {
            try {
                thread.interrupt();
                return true;
            } finally {
                IOUtils.closeQuietly(closeable);
                closeable = null;
            }
        }
        return false;
    }

    @JsonIgnore
    public void setCloseable(Closeable closeable) {
        this.closeable = closeable;
    }

    @JsonIgnore
    public boolean isRunning() {
        return getLastStage() instanceof RunningStage runningStage && runningStage.isRunning();
    }

    @JsonIgnore
    public boolean isFinal() {
        return getLastStage() instanceof FinalStage finalStage && finalStage.isFinal();
    }

    @JsonIgnore
    public boolean isPaused() {
        return isPaused(getLastStage());
    }

    @JsonIgnore
    private boolean isPaused(Stage stage) {
        return stage instanceof RunningStage runningStage && runningStage.isPaused();
    }

    @JsonIgnore
    public Integer getIndex() {
        return Optional.ofNullable(getLastStage())
                .map(stage -> stage instanceof RunningStage rStage ? rStage.getIndex() : null)
                .orElse(null);
    }

    @JsonIgnore
    public void addPrepareStage() {
        addStage(new PrepareStage());
    }

    @JsonIgnore
    public void addPullStage() {
        addStage(new PullStage());
    }

    @JsonIgnore
    public void addBuildStage() {
        addStage(new BuildStage());
    }

    @JsonIgnore
    public void addDeployStage() {
        addStage(new DeployStage());
    }

    @JsonIgnore
    public void addAbortStage() {
        addStage(new AbortStage());
    }

    @JsonIgnore
    public void addSuccessStage() {
        addStage(new SuccessStage());
    }

    @JsonIgnore
    public void addFailureStage() {
        addStage(new FailureStage());
    }

    @JsonIgnore
    private void addStage(Stage stage) {
        synchronized (lock) {
            if (stages == null) {
                stages = new ArrayList<>(5);
            }
            if (getLastStage() instanceof RunningStage runningStage) {
                RunningStage.Time lastTime = runningStage.getLastTime();
                if (lastTime.getEndTime() == null) {
                    lastTime.setEndTime(LocalDateTime.now());
                }
            }

            stages.add(stage);
            appendLog(stage, stage.getLabel(), null);
        }
    }

    @JsonIgnore
    public Stage getLastStage() {
        synchronized (lock) {
            if (CollectionUtils.isNotEmpty(stages)) {
                return stages.getLast();
            }
            return null;
        }
    }

    // 获取开始时间
    @JsonIgnore
    public LocalDateTime getStartTime() {
        synchronized (lock) {
            if (CollectionUtils.isNotEmpty(stages)) {
                Stage stage = stages.getFirst();
                if (stage instanceof RunningStage runningStage) {
                    return runningStage.getLastTime().getStartTime();
                }
            }
            return null;
        }
    }

    @JsonIgnore
    public Status getStatus() {
        synchronized (lock) {
            Stage stage = getLastStage();
            if (stage == null) {
                return null;
            }
            return Status.builder()
                    .code(stage.getCode())
                    .label(stage.getLabel())
                    .detail(getStages().stream().map(Stage::getText).collect(Collectors.joining(" -> ")))
                    .build();
        }
    }

    // 获取耗时
    @JsonIgnore
    public Duration getDuration() {
        synchronized (lock) {
            if (CollectionUtils.isNotEmpty(stages)) {
                return stages.stream()
                        .map(stage -> stage instanceof RunningStage rStage ? rStage.getDuration() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                        .stream()
                        .reduce(Duration::plus)
                        .orElse(Duration.ZERO);
            }
            return null;
        }
    }

    @JsonIgnore
    public void appendLog(String entry, Consumer<Outer> outer) {
        appendLog(getLastStage(), entry, outer);
    }

    @JsonIgnore
    private void appendLog(Stage stage, String entry, Consumer<Outer> outer) {
        String code = stage.getCode();
        appendLog(String.format("<span class=\"entry %s\">%s</span>",
                code,
                StringEscapeUtils.escapeHtml4(String.format("%s [%s] %s", DateTimeUtil.format(LocalDateTime.now(), "yyyy/MM/dd HH:mm:ss.SSS"), code.toUpperCase(), entry))));

        if (outer != null) {
            try {
                appendLog(String.format("<span class=\"out %s\">", code));
                outer.accept(this::appendEscapedLnLog);
            } finally {
                appendLog("</span>");
            }
        }

        appendLnLog("");
    }

    @JsonIgnore
    private void appendEscapedLnLog(String content) {
        appendLnLog(StringEscapeUtils.escapeHtml4(content));
    }

    @JsonIgnore
    @SneakyThrows
    private void appendLnLog(String content) {
        appendLog(content + '\n');
    }

    @JsonIgnore
    @SneakyThrows
    private void appendLog(String content) {
        synchronized (lock) {
            Files.writeString(logFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (!interrupted && Thread.currentThread().isInterrupted()) {
                interrupted = true;
                throw new InterruptedException();
            }
        }
    }

}
