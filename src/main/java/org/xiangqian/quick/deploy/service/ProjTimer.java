package org.xiangqian.quick.deploy.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xiangqian.quick.deploy.model.*;
import org.xiangqian.quick.deploy.model.Record;
import org.xiangqian.quick.deploy.util.DateTimeUtil;
import org.xiangqian.quick.deploy.util.DurationUtil;
import org.xiangqian.quick.deploy.util.Git;
import org.xiangqian.quick.deploy.util.SecurityUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author xiangqian
 * @date 2026/07/01 21:10
 */
@Slf4j
@Component
public class ProjTimer implements ApplicationRunner {

    @Autowired
    private ProjService projService;

    @Autowired
    private EmitterService emitterService;

    private long lastTime = 0;

    private void sendEvent(EmitterService.Emitter emitter) {
        String groupId = emitter.getGroupId();
        Group group = projService.groups.get(groupId);
        if (group == null) {
            return;
        }

        Collection<Proj> projs = group.getProjs();
        if (CollectionUtils.isEmpty(projs)) {
            return;
        }

        for (Proj proj : projs) {
            Record lastRecord = proj.getLastRecord();
            if (lastRecord == null) {
                continue;
            }

            emitter.send("proj", Map.of("id", proj.getId(),
                    "todayRecordCount", proj.getTodayRecordCount(),
                    "lastRecord", Map.of("id", lastRecord.getId(),
                            "running", lastRecord.isRunning(),
                            "final", lastRecord.isFinal(),
                            "paused", lastRecord.isPaused(),
                            "commit", SummaryDetail.builder().summary(Optional.ofNullable(lastRecord.getCommit()).map(Git.Commit::getSummary).orElse(null)).detail(Optional.ofNullable(lastRecord.getCommit()).map(Git.Commit::getDetail).orElse(null)).build(),
                            "operator", SummaryDetail.builder().summary(StringUtils.join(lastRecord.getOperatorNicks(), "、")).detail(lastRecord.joinOperators("、")).build(),
                            "time", DateTimeUtil.human(lastRecord.getStartTime()),
                            "status", lastRecord.getStatus(),
                            "duration", DurationUtil.human(lastRecord.getDuration()))));
        }
    }

    private void sendEvent() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);

                if (emitterService.isEmpty()) {
                    continue;
                }

                long currTime = System.currentTimeMillis();
                if (currTime > lastTime + 5 * 1000) {
                    lastTime = currTime;
                    log.debug("emitters {}", emitterService);
                }

                emitterService.sendAll(emitter -> {
                    try {
                        sendEvent(emitter);
                    } catch (Exception e) {
                        log.error("【Timer-sendEvent】emitter{uuid={}, key={}} 异常", emitter.getUuid(), emitter.getKey(), e);
                    }
                });
            } catch (Exception e) {
                log.error("【Timer-sendEvent】异常", e);
            }
        }
    }

    private void polling(Group group, Proj proj) {
        Trigger.Polling polling = Optional.ofNullable(proj.getTrigger()).map(Trigger::getPolling).orElse(null);
        if (polling == null) {
            return;
        }

        int interval = Optional.ofNullable(polling.getInterval()).orElse(0);
        if (interval <= 0) {
            return;
        }

        long lastTime = polling.getLastTime();
        long currTime = System.currentTimeMillis();
        if (currTime >= lastTime + interval * 1000) {
            polling.setLastTime(currTime);

            Repo repo = Optional.ofNullable(proj.getGit()).map(org.xiangqian.quick.deploy.model.Git::getRepo).orElse(null);
            String localLastCommitId = Optional.ofNullable(repo.log(1))
                    .filter(CollectionUtils::isNotEmpty)
                    .map(List::getFirst)
                    .map(Git.Commit::getId)
                    .orElse(null);
            String remoteLastCommitId = repo.remoteLastCommitId();
            if (!StringUtils.equals(localLastCommitId, remoteLastCommitId)) {
                try {
                    SecurityUtil.setWebhookUser();
                    projService.deploy(group.getId(), proj.getId(), "HEAD", $ -> true);
                } finally {
                    SecurityUtil.removeWebhookUser();
                }
            }
        }
    }

    private void polling() {
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (Exception e) {
            log.error("【Timer-polling】sleep异常", e);
        }
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
                for (Group group : projService.groups.values()) {
                    for (Proj proj : group.getProjs()) {
                        try {
                            polling(group, proj);
                        } catch (Exception e) {
                            log.error("【Timer-polling】groupId={}, projId={} 异常", group.getId(), proj.getId(), e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("【Timer-polling】异常", e);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this::sendEvent, "Timer-sendEvent").start();
        new Thread(this::polling, "Timer-polling").start();
    }

}
