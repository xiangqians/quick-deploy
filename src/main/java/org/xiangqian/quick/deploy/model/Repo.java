package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.xiangqian.quick.deploy.util.Git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 仓库
 *
 * @author xiangqian
 * @date 2026/01/19 12:10
 */
@Data
public class Repo {
    // 地址
    private String url;
    // 分支列表
    private List<String> branches;
    // 用户
    private String user;
    // 密码
    private String passwd;

    // 最近提交
    @JsonIgnore
    private List<Git.Commit> lastCommits;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Git git;

    @SneakyThrows
    public void init(Path dir) {
        if (!open(dir)) {
            clone(dir);
        }
    }

    private boolean open(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return false;
        }

        try {
            git = Git.open(dir, user, passwd);
        } catch (RepositoryNotFoundException e) {
            IOUtils.closeQuietly(git);
            PathUtils.deleteDirectory(dir);
            return false;
        }

        if (!git.remote().equals(url)) {
            IOUtils.closeQuietly(git);
            PathUtils.deleteDirectory(dir);
            return false;
        }

        String branch = branches.get(0);
        if (!git.branch().equals(branch)) {
            git.reset("HEAD", $ -> {
            });
//            git.pull($ -> {
//            });
            git.checkout(branch, $ -> {
            });
        }

        return true;
    }

    private void clone(Path dir) throws Exception {
        String branch = branches.get(0);
        git = Git.clone(dir, url, branch, user, passwd);
    }

    public Path getDir() {
        return git.getDir();
    }

    @SneakyThrows
    public void pull(Consumer<String> consumer) {
        git.pull(consumer);
    }

    @SneakyThrows
    public void reset(String commitId, Consumer<String> consumer) {
        git.reset(commitId, consumer);
    }

    @SneakyThrows
    public List<Git.Commit> log(int maxCount) {
        return git.log(maxCount);
    }

    @SneakyThrows
    public List<Git.Commit> log(String commitId, int maxCount) {
        return git.log(commitId, maxCount);
    }

}
