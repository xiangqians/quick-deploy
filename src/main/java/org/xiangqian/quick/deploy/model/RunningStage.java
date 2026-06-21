package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.xiangqian.quick.deploy.util.DurationUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author xiangqian
 * @date 2026/04/22 17:35
 */
public abstract class RunningStage extends Stage {

    @Getter
    @Setter
    private List<Time> times;

    @JsonIgnore
    private Cmd cmd;

    public RunningStage(String code, String name) {
        super(code, name);
        this.times = new ArrayList<>(2);
        this.times.add(Time.builder().startTime(LocalDateTime.now()).build());
    }

    @JsonIgnore
    public boolean pause(Cmd cmd) {
        this.cmd = cmd;
        addTime(new Time());
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isRunning() {
        return getLastTime().getEndTime() == null;
    }

    @JsonIgnore
    @Override
    public boolean isPaused() {
        return getLastTime().getStartTime() == null;
    }

    @JsonIgnore
    public Integer getIndex() {
        return Optional.ofNullable(cmd).map(Cmd::getIndex).orElse(null);
    }

    @JsonIgnore
    public Time getLastTime() {
        return times.getLast();
    }

    @JsonIgnore
    public void removeLastTime() {
        times.removeLast();
    }

    @JsonIgnore
    public void addTime(Time time) {
        getLastTime().setEndTime(LocalDateTime.now());
        times.add(time);
    }

    @JsonIgnore
    public Duration getDuration() {
        return times.stream()
                .filter(time -> Objects.nonNull(time.getStartTime()))
                .map(time -> Duration.between(time.getStartTime(), Optional.ofNullable(time.getEndTime()).orElse(LocalDateTime.now())))
                .reduce(Duration::plus)
                .orElse(Duration.ZERO);
    }

    @JsonIgnore
    @Override
    public String getLabel() {
        String label = name;
        if (isRunning()) {
            if (isPaused()) {
                label += "已暂停<br>@ " + cmd.getName();
            } else {
                label += "中";
            }
        }
        return label;
    }

    @JsonIgnore
    @Override
    public String getText() {
        String label = name;

        boolean paused = false;
        if (isRunning()) {
            paused = isPaused();
            if (paused) {
                label += "已暂停";
            } else {
                label += "中";
            }
        }
        return String.format("%s（%s）%s",
                label,
                DurationUtil.human(getDuration()),
                (paused ? ("@ " + cmd.getName()) : ""));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Time {
        // 开始时间
        private LocalDateTime startTime;
        // 结束时间
        private LocalDateTime endTime;
    }

}
