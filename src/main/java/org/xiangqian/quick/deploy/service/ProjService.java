package org.xiangqian.quick.deploy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpSession;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xiangqian.quick.deploy.model.*;
import org.xiangqian.quick.deploy.model.Record;
import org.xiangqian.quick.deploy.util.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目服务
 *
 * @author xiangqian
 * @date 2026/01/19 12:18
 */
@Slf4j
@Service
public class ProjService implements ApplicationRunner, Runnable {

    private Map<String, Group> groups;

    @Autowired
    private EmitterService emitterService;

    @Autowired
    private UserService userService;

    public ProjService(@Value("${server.port}") Integer port, @Value("${server.servlet.context-path}") String contextPath, @Value("${dir}") String dir) throws Exception {
        // 加载项目组信息
        groups = YamlUtil.deser(Path.of(dir, "proj.yml").toFile(), new TypeReference<List<Group>>() {
                }).stream()
                .collect(Collectors.toMap(Group::getId, Function.identity(), (oldGroup, newGroup) -> {
                    throw new IllegalStateException(String.format("Duplicate group ids [{id=%s, name=%s}, {id=%s, name=%s}]",
                            oldGroup.getId(), oldGroup.getName(),
                            newGroup.getId(), newGroup.getName()));
                }, LinkedHashMap::new));

        StringBuilder webhook = new StringBuilder();
        for (Group group : groups.values()) {
            for (Proj proj : group.getProjs()) {
                proj.setGroupId(group.getId());
                proj.setGroupName(group.getName());
                proj.setDir(dir);

                // Webhook（网络钩子）
                webhook.append(String.format("%s\nhttp://localhost:%s%s/proj/%s/%s/deploy/webhook?token=%s\n\n", proj.getName(), port, ("/".equals(contextPath) ? "" : contextPath), group.getId(), proj.getId(), proj.getToken()));

                // 初始化仓库
                log.debug("初始化本地仓库 { groupId={}, projId={}, projName={} } ...", group.getId(), proj.getId(), proj.getName());
                Repo repo = proj.getRepo();
                repo.init(proj.getDir("repo"));
                log.debug("已初始化本地仓库 { groupId={}, projId={}, projName={} }", group.getId(), proj.getId(), proj.getName());

                // 加载记录列表
                Path recordsJsonFile = proj.getRecordsJsonFile();
                if (Files.exists(recordsJsonFile)) {
                    proj.setRecords(new LinkedList<>(JsonUtil.deser(Files.readAllBytes(recordsJsonFile), new TypeReference<List<Record>>() {
                    })));

                    Record lastRecord = proj.getLastRecord();
                    if (lastRecord != null) {
                        lastRecord.setLogFile(proj.getRecordLogFile(lastRecord));
                        if (!lastRecord.isFinal()) {
                            lastRecord.addFailureStage();
                            proj.writeRecordsJsonFile();
                        }
                    }
                } else {
                    proj.setRecords(new LinkedList<>());
                }
            }
        }

        Files.writeString(Path.of(dir, "webhook.txt"), webhook);
    }

    private Proj getProj(String groupId, String projId) {
        Group group = groups.get(groupId);
        if (group != null) {
            return group.getProj(projId);
        }
        return null;
    }

    public Map<String, Object> list(String groupId) {
        groupId = StringUtils.trim(groupId);
        HttpSession session = WebUtil.getSession();
        if (StringUtils.isEmpty(groupId)) {
            if (session.getAttribute("group") instanceof Group group) {
                groupId = group.getId();
            } else if (MapUtils.isNotEmpty(groups)) {
                groupId = groups.keySet().iterator().next();
            }
        }
        Group group = Group.builder()
                .id(groupId)
                .name(Optional.ofNullable(groups.get(groupId)).map(Group::getName).orElse(null))
                .build();
        session.setAttribute("group", group);

        Map<String, Object> map = new HashMap<>(3, 1f);
        map.put("group", group);
        map.put("groups", groups.values().stream().map(Group::copy).collect(Collectors.toList()));

        Collection<Proj> projs = Optional.ofNullable(groups.get(groupId)).map(Group::getProjs).orElse(null);
        if (CollectionUtils.isNotEmpty(projs)) {
            for (Proj proj : projs) {
                Repo repo = proj.getRepo();
                List<Git.Commit> commits = repo.log(20);
                repo.setLastCommits(commits);
            }
            map.put("projs", projs);
        }

        return map;
    }

    public List<Git.Commit> prevCommits(String groupId, String projId, String commitId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null) {
            return Collections.emptyList();
        }

        Repo repo = proj.getRepo();
        List<Git.Commit> commits = repo.log(commitId, 11);
        if (CollectionUtils.isNotEmpty(commits)) {
            Git.Commit commit = commits.getFirst();
            if (StringUtils.equals(commit.getId(), commitId)) {
                commits.remove(0);
            }
        }
        return commits;
    }

    public Boolean pull(String groupId, String projId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null || !proj.tryLock()) {
            return false;
        }

        try {
            Repo repo = proj.getRepo();
            repo.reset("HEAD", log::debug);
            repo.pull(log::debug);
            return true;
        } finally {
            proj.unlock();
        }
    }

    @SneakyThrows
    public Boolean deploy(String groupId, String projId, String commitId, Function<Proj, Boolean> validator) {
        Proj proj = getProj(groupId, projId);
        if (proj == null || !Boolean.TRUE.equals(validator.apply(proj))) {
            return false;
        }

        boolean locked = proj.tryLock();
        Record lastRecord = proj.getLastRecord();
        String operator = SecurityUtil.getUser().getUsername();
        String firstOperator = Optional.ofNullable(lastRecord).map(Record::getFirstOperator).map(User::getName).orElse(null);
        if (!locked) {
            if ("webhook".equals(operator)
                    && operator.equals(firstOperator)
                    && Boolean.TRUE.equals(Optional.ofNullable(lastRecord).map(Record::isRunning).orElse(null))) {
                abort(proj);
                asyncDeploy(groupId, projId, commitId, validator);
                return true;
            }
            return false;
        }

        if ("webhook".equals(operator)
                && operator.equals(firstOperator)
                && Boolean.TRUE.equals(Optional.ofNullable(lastRecord).map(Record::isPaused).orElse(null))) {
            proj.unlock();
            abort(proj);
            asyncDeploy(groupId, projId, commitId, validator);
            return true;
        }

        if (Boolean.FALSE.equals(Optional.ofNullable(lastRecord).map(Record::isFinal).orElse(null))) {
            proj.unlock();
            return false;
        }

        // step1：准备
        Record record = new Record();
        proj.addRecord(record);
        record.setId(UuidUtil.random());
        record.setLogFile(proj.getRecordLogFile(record));
        record.addPrepareStage();
        record.setCommitId(commitId);
        record.addOperator(userService.getByName(operator));
        record.setCloseable(null);
        record.setTargets(null);

        // 异步部署
        asyncDeploy(proj, record);

        proj.writeRecordsJsonFile();
        return true;
    }

    private void asyncDeploy(String groupId, String projId, String commitId, Function<Proj, Boolean> validator) {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                SecurityUtil.setWebhookUser();
                deploy(groupId, projId, commitId, validator);
            } catch (InterruptedException e) {
            }
        }).start();
    }

    private void asyncDeploy(Proj proj, Record record) {
        Thread thread = new Thread(() -> {
            try {
                // step2：拉取远程仓库代码
                pull(proj, record);

                //step3：构建
                build(proj, record);

                // step4：部署
                deploy(proj, record);
            } catch (Exception e) {
                onError(proj, record, e);
            } finally {
                proj.unlock();
                proj.writeRecordsJsonFile();
            }
        });
        record.setThread(thread);
        record.setInterrupted(false);
        record.setAborted(false);
        thread.start();
    }

    private void pull(Proj proj, Record record) {
        Stage lastStage = record.getLastStage();
        if (lastStage == null || !lastStage.isPrepare()) {
            return;
        }

        record.addPullStage();
        Repo repo = proj.getRepo();
        repo.reset("HEAD", $ -> {
        });
        String commitId = record.getCommitId();
        if ("HEAD".equals(commitId)) {
            record.appendLog("Local$ git pull", outer -> repo.pull(outer::outLn));
        } else {
            record.appendLog(String.format("Local$ git reset --hard %s", commitId), outer -> repo.reset(commitId, outer::outLn));
        }
        record.setCommit(Optional.ofNullable(repo.log(1)).filter(CollectionUtils::isNotEmpty).map(List::getFirst).orElseThrow());
    }

    private void build(Proj proj, Record record) throws Exception {
        Stage lastStage = null;
        Build build = proj.getBuild();
        if (build == null || (lastStage = record.getLastStage()) == null) {
            return;
        }

        Integer index = null;
        if (lastStage.isPull()) {
            record.addBuildStage();
        } else if (lastStage.isBuild() && (index = record.getIndex()) != null) {

        } else {
            return;
        }

        // 获取本地仓库目录
        Path repoDir = proj.getRepo().getDir();

        // 执行命令
        if (!exec(record, build, index, cmd -> {
            try {
                record.appendLog(String.format("Local$ %s", cmd), outer -> CmdUtil.exec(String.format("cd %s && %s", repoDir.toString(), cmd), record::setCloseable, outer::out));
            } finally {
                record.setCloseable(null);
            }
        })) {
            return;
        }

        // 获取目标文件
        if (CollectionUtils.isNotEmpty(build.getTargets())) {
            Map<String, File> targets = new LinkedHashMap<>();
            for (String target : build.getTargets()) {
                targets.put(target, null);
            }
            for (Map.Entry<String, File> target : targets.entrySet()) {
                Path path = repoDir.resolve(target.getKey());
                if (Files.exists(path)) {
                    target.setValue(path.toFile());
                }
            }

            List<String> ntargets = targets.entrySet().stream()
                    .filter(entry -> entry.getValue() == null)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ntargets)) {
                throw new Exception("未找到目标文件：" + ntargets.stream().collect(Collectors.joining("、")));
            }
            record.setTargets(targets);
        }
    }

    @SneakyThrows
    private void deploy(Proj proj, Record record) {
        Stage lastStage = null;
        Deploy deploy = proj.getDeploy();
        if (deploy == null || (lastStage = record.getLastStage()) == null) {
            return;
        }

        Integer index = null;
        if (lastStage.isPull() || lastStage.isBuild()) {
            record.addDeployStage();
        } else if (lastStage.isDeploy() && (index = record.getIndex()) != null) {

        } else {
            return;
        }

        Sftp sftp = null;
        File zipFile = null;
        try {
            Server server = proj.getServer();
            sftp = new Sftp(server.getHost(), server.getPort(), server.getUser(), server.getPasswd(), Duration.ofMinutes(2));

            // 远程服务器项目路径
            String projDir = String.format("quick-deploy/%s", proj.getId());

            Sftp finalSftp = sftp;
            if (index == null) {
                // 删除远程服务器项目目录
                String cmd = String.format("rm -rf %s", projDir);
                try {
                    String finalCmd = cmd;
                    record.appendLog(String.format("Remote$ %s", cmd), outer -> finalSftp.exec(finalCmd, Duration.ofSeconds(30), record::setCloseable, outer::out));
                } finally {
                    record.setCloseable(null);
                }

                // 创建远程服务器项目目录
                cmd = String.format("mkdir -p %s", projDir);
                try {
                    String finalCmd = cmd;
                    record.appendLog(String.format("Remote$ %s", cmd), outer -> finalSftp.exec(finalCmd, Duration.ofSeconds(30), record::setCloseable, outer::out));
                } finally {
                    record.setCloseable(null);
                }

                // 进入远程服务器项目目录
                record.appendLog(String.format("Remote$ cd %s", projDir), null);
                sftp.cd(projDir);

                Map<String, File> targets = record.getTargets();
                if (MapUtils.isNotEmpty(targets)) {
                    // 压缩目标文件
                    zipFile = proj.getDir().resolve("tmp.zip").toFile();
                    record.appendLog(String.format("Local$ zip -r %s %s", zipFile.getName(), targets.keySet().stream().collect(Collectors.joining(" "))), null);
                    ZipUtil.compress(null, targets.values().stream().collect(Collectors.toList()), zipFile);

                    // 上传zip
                    File finalZipFile = zipFile;
                    record.appendLog(String.format("Local$ put %s %s", zipFile.getName(), zipFile.getName()), outer -> finalSftp.put(finalZipFile.getAbsolutePath(), finalZipFile.getName(), outer::out));

                    // 解压zip
                    cmd = String.format("cd %s && unzip -o %s", projDir, zipFile.getName());
                    try {
                        String finalCmd = cmd;
                        record.appendLog(String.format("Remote$ unzip -o %s", zipFile.getName()), outer -> finalSftp.exec(finalCmd, Duration.ofMinutes(2), record::setCloseable, outer::out));
                    } finally {
                        record.setCloseable(null);
                    }

                    // 删除zip
                    cmd = String.format("cd %s && rm -rf %s", projDir, zipFile.getName());
                    try {
                        String finalCmd = cmd;
                        record.appendLog(String.format("Remote$ rm -rf %s", zipFile.getName()), outer -> finalSftp.exec(finalCmd, Duration.ofMinutes(2), record::setCloseable, outer::out));
                    } finally {
                        record.setCloseable(null);
                    }

                    // 查看文件列表
                    cmd = String.format("cd %s && ls -l", projDir);
                    try {
                        String finalCmd = cmd;
                        record.appendLog("Remote$ ls -l", outer -> finalSftp.exec(finalCmd, Duration.ofMinutes(2), record::setCloseable, outer::out));
                    } finally {
                        record.setCloseable(null);
                    }
                }
            }

            // 执行远程命令
            if (!exec(record, deploy, index, cmd -> {
                try {
                    record.appendLog(String.format("Remote$ %s", String.format("cd %s && %s", projDir, cmd)), outer -> finalSftp.exec(String.format("cd %s && %s", projDir, cmd), Duration.ofSeconds(30), record::setCloseable, outer::out));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    record.setCloseable(null);
                }
            })) {
                return;
            }

            record.addSuccessStage();
        } finally {
            IOUtils.closeQuietly(sftp);
            if (zipFile != null) {
                FileUtils.forceDelete(zipFile);
            }
        }
    }

    private boolean exec(Record record, AbsCmds absCmds, Integer index, Consumer<String> cmdHandler) {
        List<Cmd> cmds = absCmds.getCmds();
        if (CollectionUtils.isEmpty(cmds)) {
            return true;
        }
        for (int i = Optional.ofNullable(index).orElse(0), size = cmds.size(); i < size; i++) {
            Cmd cmd = cmds.get(i);
            if (!exec(record, cmd, Integer.valueOf(i).equals(index), cmdHandler)) {
                return false;
            }
        }
        return true;
    }

    private boolean exec(Record record, Cmd cmd, boolean paused, Consumer<String> cmdHandler) {
        String pause = cmd.getPause();
        if (!paused && pause != null) {
            if (SpelUtil.evaluate(pause, Map.of("operatorName", record.getFirstOperator().getName()), Boolean.class)) {
                record.pause(cmd);
                return false;
            }
        }
        cmdHandler.accept(cmd.getRun());
        return true;
    }

    private void onError(Proj proj, Record record, Exception e) {
        log.error("“{}”部署异常", proj.getName(), e);
        if (record.isAborted()) {
            record.addAbortStage();
        } else {
            record.addFailureStage();
        }

        record.appendLog(Optional.ofNullable(e.getMessage()).orElse(e.getClass().getName()), outer -> {
            try (PrintWriter writer = IoUtil.newPrintWriter(outer::out)) {
                e.printStackTrace(writer);
            }
        });
    }

    public Boolean resume(String groupId, String projId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null || !proj.tryLock()) {
            return false;
        }

        Record record = proj.getLastRecord();
        if (record.resume()) {
            record.addOperator(userService.getByName(SecurityUtil.getUser().getUsername()));
            asyncDeploy(proj, record);
            return true;
        }

        proj.unlock();
        return false;
    }

    public Boolean abort(String groupId, String projId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null) {
            return false;
        }
        return abort(proj);
    }

    private Boolean abort(Proj proj) {
        Record record = proj.getLastRecord();
        if (record != null) {
            record.addOperator(userService.getByName(SecurityUtil.getUser().getUsername()));
            if (record.abort()) {
                proj.writeRecordsJsonFile();
                return true;
            }
            return false;
        }
        return false;
    }

    @SneakyThrows
    public Map<String, Object> recordList(String groupId, String projId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null) {
            throw new Exception("项目不存在");
        }
        Map<String, Object> map = new HashMap<>(5, 1f);
        map.put("projId", proj.getId());
        map.put("projName", proj.getName());
        map.put("groupId", proj.getGroupId());
        map.put("groupName", proj.getGroupName());
        map.put("records", proj.getRecords());
        return map;
    }

    @SneakyThrows
    public Map<String, Object> recordLog(String groupId, String projId, String recordId) {
        Proj proj = getProj(groupId, projId);
        if (proj == null) {
            throw new Exception("项目不存在");
        }

        Map<String, Object> map = new HashMap<>(4, 1f);
        map.put("groupName", proj.getGroupName());
        map.put("projName", proj.getName());
        LocalDateTime startTime = null;

        // 日志文件
        Record record = proj.getRecord(recordId);
        if (record != null) {
            Path logFile = proj.getRecordLogFile(record);
            if (Files.exists(logFile)) {
                map.put("content", Files.readString(logFile, StandardCharsets.UTF_8));
            }
            startTime = record.getStartTime();
        }

        map.put("startTime", Optional.ofNullable(startTime).map(DateTimeUtil::format).orElse(null));
        return map;
    }

    public SseEmitter event() {
        return emitterService.create(SecurityUtil.getUser().getUsername());
    }

    // 发送消息给所有客户端
    private void run0() {
        if (emitterService.isEmpty()) {
            return;
        }
        Collection<Proj> projs = Collections.emptyList();
        for (Proj proj : projs) {
            Record lastRecord = proj.getLastRecord();
            if (lastRecord == null) {
                continue;
            }

            emitterService.sendAll("proj", Map.of("id", proj.getId(),
                    "todayRecordCount", proj.getTodayRecordCount(),
                    "lastRecord", Map.of("running", lastRecord.isRunning(),
                            "final", lastRecord.isFinal(),
                            "paused", lastRecord.isPaused(),
                            "commit", SummaryDetail.builder()
                                    .summary(Optional.ofNullable(lastRecord.getCommit()).map(Git.Commit::getSummary).orElse(null))
                                    .detail(Optional.ofNullable(lastRecord.getCommit()).map(Git.Commit::getDetail).orElse(null))
                                    .build(),
                            "operator", SummaryDetail.builder()
                                    .summary(StringUtils.join(lastRecord.getOperatorNicks(), "、"))
                                    .detail(lastRecord.joinOperators("、"))
                                    .build(),
                            "time", DateTimeUtil.human(lastRecord.getStartTime()),
                            "status", lastRecord.getStatus(),
                            "duration", DurationUtil.human(lastRecord.getDuration()))
            ));
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                run0();
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                log.error("proj thread run 异常", e);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(this).start();
    }

}
