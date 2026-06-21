package org.xiangqian.quick.deploy.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * jgit demo
 * https://github.com/centic9/jgit-cookbook
 *
 * @author xiangqian
 * @date 2024/03/13 23:20
 */
public class Git implements Closeable {

    private Path dir;
    private org.eclipse.jgit.api.Git git;

    // 在每个执行网络操作（如：clone、pull、push）命令时需要设置凭证
    private CredentialsProvider credsProvider;

    /**
     * @param dir
     * @param git           {@link org.eclipse.jgit.api.Git}
     * @param credsProvider 凭证
     */
    private Git(Path dir, org.eclipse.jgit.api.Git git, CredentialsProvider credsProvider) {
        this.dir = dir;
        this.git = git;
        this.credsProvider = credsProvider;
    }

    /**
     * $ git clone <uri> -b <branch>
     *
     * @param dir    本地目录
     * @param url    仓库地址
     * @param branch 分支
     * @param user   用户
     * @param passwd 密码
     * @return
     * @throws Exception
     */
    public static Git clone(Path dir, String url, String branch, String user, String passwd) throws Exception {
        // 用户、密码凭证
        CredentialsProvider credsProvider = null;
        if (StringUtils.isNoneEmpty(user, passwd)) {
            credsProvider = new UsernamePasswordCredentialsProvider(user, passwd);
        }

        // JGit
        org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.cloneRepository()
                // 本地目录
                .setDirectory(dir.toFile())
                // 仓库地址
                .setURI(url)
                // 分支名
                .setBranch(branch)
                // 凭证
                .setCredentialsProvider(credsProvider)
                .call();

        return new Git(dir, git, credsProvider);
    }

    /**
     * @param dir    本地目录
     * @param user   用户
     * @param passwd 密码
     * @return
     * @throws Exception
     */
    public static Git open(Path dir, String user, String passwd) throws Exception {
        // 用户、密码凭证
        CredentialsProvider credsProvider = null;
        if (StringUtils.isNoneEmpty(user, passwd)) {
            credsProvider = new UsernamePasswordCredentialsProvider(user, passwd);
        }

        return new Git(dir, org.eclipse.jgit.api.Git.open(dir.toFile()), credsProvider);
    }

    public Path getDir() {
        return dir;
    }

    /**
     * $ git branch
     *
     * @return
     * @throws Exception
     */
    public String branch() throws Exception {
        return getRepo().getBranch();
    }

    /**
     * $ git pull
     *
     * @param consumer
     * @throws Exception
     */
    public void pull(Consumer<String> consumer) throws Exception {
        PullResult result = git.pull()
                .setRemoteBranchName(getRepo().getBranch())
                .setCredentialsProvider(credsProvider)
                .call();
        MergeResult mergeResult = result.getMergeResult();
        consumer.accept(String.valueOf(mergeResult.getMergeStatus()));
        ObjectId[] mergedCommits = mergeResult.getMergedCommits();
        if (ArrayUtils.getLength(mergedCommits) == 2) {
            parseCommits(mergedCommits[0], mergedCommits[1]).forEach(commit -> consumer.accept(String.valueOf(commit)));
        }
    }

    public Commit parseCommit(ObjectId commitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(getRepo())) {
            return convert(revWalk.parseCommit(commitId));
        }
    }

    public Commit parseCommit(String commitId) throws IOException {
        return parseCommit(resolve(commitId));
    }

    public List<Commit> parseCommits(ObjectId startCommitId, ObjectId endCommitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(getRepo())) {
            // 标记排除起始提交（获取的是从start到end之间，不包含start）
            RevCommit startRevCommit = revWalk.parseCommit(startCommitId);
            revWalk.markUninteresting(startRevCommit);

            // 标记结束提交
            RevCommit endRevCommit = revWalk.parseCommit(endCommitId);
            revWalk.markStart(endRevCommit);

            // 遍历提交
            List<Commit> commits = new ArrayList<>();
            for (RevCommit revCommit : revWalk) {
                commits.add(convert(revCommit));
            }
            return commits;
        }
    }

    public List<Commit> parseCommits(String startCommitId, String endCommitId) throws IOException {
        return parseCommits(resolve(startCommitId), resolve(endCommitId));
    }

    /**
     * $ git reset --hard <commit-id>
     *
     * @param ref
     * @param consumer
     * @throws Exception
     */
    public void reset(String ref, Consumer<String> consumer) throws Exception {
        Ref r = git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(ref)
                .call();
        consumer.accept(parseCommit(r.getObjectId()).toString());
    }

    /**
     * $ git log -<maxCount>
     *
     * @param maxCount
     * @return
     * @throws Exception
     */
    public List<Commit> log(int maxCount) throws Exception {
        return log(null, maxCount);
    }

    public List<Commit> log(String commitId, int maxCount) throws Exception {
        LogCommand logCommand = git.log();
        if (commitId != null) {
            logCommand.add(resolve(commitId));
        }
        logCommand.setMaxCount(maxCount);
        Iterable<RevCommit> revCommitIterable = logCommand.call();
        List<Commit> commits = new ArrayList<>(maxCount);
        Iterator<RevCommit> revCommitIterator = revCommitIterable.iterator();
        while (revCommitIterator.hasNext()) {
            RevCommit revCommit = revCommitIterator.next();
            commits.add(convert(revCommit));
        }
        return commits;
    }

    private ObjectId resolve(String commitId) throws IOException {
        return getRepo().resolve(commitId);
    }

    private Commit convert(RevCommit revCommit) {
        Commit commit = new Commit();

        // 提交id
        commit.setId(revCommit.getName());

        // 提交作者
        PersonIdent authorIdent = revCommit.getAuthorIdent();
        commit.setAuthor(String.format("%s <%s>", authorIdent.getName(), authorIdent.getEmailAddress()));

        // 提交日期
        commit.setDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(revCommit.getCommitTime()), ZoneId.systemDefault()));

        // 提交信息
        commit.setMsg(revCommit.getShortMessage());

        return commit;
    }

    private Repository getRepo() {
        return git.getRepository();
    }

    @Override
    public void close() throws IOException {
        if (git != null) {
            try {
                IOUtils.closeQuietly(getRepo()::close, git::close);
            } finally {
                git = null;
            }
        }
    }

    // 提交信息
    @Data
    public static class Commit {
        // 提交id
        private String id;
        // 提交作者
        private String author;
        // 提交日期
        @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
        private LocalDateTime date;
        // 提交信息
        private String msg;

        @JsonIgnore
        public String getShortMsg() {
            String shortMsg = Optional.ofNullable(msg).orElse("-");
            if (shortMsg.length() > 15) {
                shortMsg = shortMsg.substring(0, 15) + " ...";
            }
            return shortMsg;
        }

        @JsonIgnore
        public String getSummary() {
            return Optional.ofNullable(msg).orElse("-");
        }

        @JsonIgnore
        public String getDetail() {
            return toString();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Id: ").append(id).append('\n');
            builder.append("Author: ").append(author).append('\n');
            builder.append("Date: ").append(DateTimeUtil.format(date)).append('\n');
            builder.append(msg);
            return builder.toString();
        }
    }

}
