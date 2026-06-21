package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xiangqian.quick.deploy.util.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 项目
 *
 * @author xiangqian
 * @date 2026/01/19 12:08
 */
@Data
@Slf4j
public class Proj {

    // 唯一标识
    private String id;
    // 名称
    private String name;
    // 认证令牌
    private String token;
    // 仓库
    private Repo repo;
    // 服务器
    private Server server;
    // 构建
    private Build build;
    // 部署
    private Deploy deploy;

    // 记录列表
    private List<Record> records;

    // 是否已锁定
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private volatile boolean locked;

    public void setRecords(List<Record> records) {
        this.records = records;
        records.forEach(record -> record.setLock(records));
    }

    @JsonIgnore
    public synchronized boolean tryLock() {
        if (locked) {
            return false;
        }
        locked = true;
        return true;
    }

    @JsonIgnore
    public synchronized void unlock() {
        locked = false;
    }

    @JsonIgnore
    public int getTodayRecordCount() {
        synchronized (records) {
            if (CollectionUtils.isNotEmpty(records)) {
                return (int) records.stream().filter(record -> LocalDate.now().equals(record.getStartTime().toLocalDate())).count();
            }
            return 0;
        }
    }

    @JsonIgnore
    public Record getLastRecord() {
        synchronized (records) {
            if (CollectionUtils.isNotEmpty(records)) {
                return records.getFirst();
            }
            return null;
        }
    }

    @JsonIgnore
    public void addRecord(Record record) {
        synchronized (records) {
            record.setLock(records);
            records.addFirst(record);
            while (records.size() > 100) {
                Record removedRecord = null;
                try {
                    removedRecord = records.removeLast();
                    Path recordLogFile = getRecordLogFile(removedRecord);
                    Files.deleteIfExists(recordLogFile);
                } catch (Exception e) {
                    log.error("移除记录异常 " + removedRecord, e);
                }
            }
        }
    }

    @JsonIgnore
    public Record getRecord(String id) {
        synchronized (records) {
            if (CollectionUtils.isNotEmpty(records)) {
                return records.stream().filter(record -> StringUtils.equals(record.getId(), id)).findFirst().orElse(null);
            }
            return null;
        }
    }

    @JsonIgnore
    @SneakyThrows
    public Path getRecordLogFile(Record record) {
        return getDir("log").resolve(record.getId() + ".log");
    }

    @JsonIgnore
    public Path getRecordsJsonFile() {
        return getDir().resolve("records.json");
    }

    @JsonIgnore
    @SneakyThrows
    public void writeRecordsJsonFile() {
        synchronized (records) {
            Files.writeString(getRecordsJsonFile(), JsonUtil.serToStr(records), StandardCharsets.UTF_8);
        }
    }

    @JsonIgnore
    @SneakyThrows
    public Path getDir(String... more) {
        int moreLen = more != null ? more.length : 0;
        String[] newMore = new String[1 + moreLen];
        newMore[0] = id;
        if (moreLen > 0) {
            System.arraycopy(more, 0, newMore, 1, moreLen);
        }
        Path dir = Path.of("proj", newMore);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

}
