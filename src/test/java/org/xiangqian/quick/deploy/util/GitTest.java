package org.xiangqian.quick.deploy.util;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

/**
 * @author xiangqian
 * @date 2026/01/19 11:35
 */
public class GitTest {
    private Git git;

    @Before
    public void before() throws Exception {
        String user = "myuser";
        String passwd = "mypasswd";
        String dir = "D:\\xiangqian\\project\\quick-deploy\\tmp\\proj\\test";
        git = Git.open(Path.of(dir), user, passwd);
    }

    @Test
    public void remote() throws Exception {
        System.out.format("remote: %s", git.remote()).println();
    }

    @Test
    public void branch() throws Exception {
        System.out.format("branch: %s", git.branch()).println();
    }

    @Test
    public void checkout() throws Exception {
        String branch = "dev";
        git.checkout(branch, System.out::println);
    }

    @Test
    public void pull() throws Exception {
        git.pull(str -> System.out.println("\n" + str));
    }

    @Test
    public void parseCommit() throws Exception {
        String commitId = "8709a3024a2603ba3cf2c6339ec0c68a8cee06b9";
        System.out.println(git.parseCommit(commitId));
    }

    @Test
    public void parseCommits() throws Exception {
        String startCommitId = "75e18d04a19d1a56b13be6c7e5bc8b07c1c4a111";
        String endCommitId = "4d851ab0624ce4e3edf1a88a2c9dbe9f7b6f89ff";
        git.parseCommits(startCommitId, endCommitId).forEach(commit -> System.out.print(commit + "\n"));
    }

    @Test
    public void reset() throws Exception {
        String commitId = "df57331290bea8025aaa475c0a837e7c8f333c3f";
//        commitId = "HEAD";
        git.reset(commitId, System.out::println);
    }

    @Test
    public void log() throws Exception {
        List<Git.Commit> commits = git.log(2);
        commits.forEach(commit -> System.out.println(commit + "\n"));
    }

    @Test
    public void log2() throws Exception {
//        String commitId = "75e18d04a19d1a56b13be6c7e5bc8b07c1c4a111";
//        String commitId = "4d851ab0624ce4e3edf1a88a2c9dbe9f7b6f89ff";
        String commitId = "HEAD";
        git.log(commitId, 2).forEach(commit -> System.out.println(commit + "\n"));
    }

}
