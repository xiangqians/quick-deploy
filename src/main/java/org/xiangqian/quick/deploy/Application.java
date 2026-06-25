package org.xiangqian.quick.deploy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Test
 * 1) Edit Configurations...
 * 2) Program arguments
 * --server.port=58088 --dir=./tmp
 *
 * @author xiangqian
 * @date 2026/01/19 11:06
 */
@ServletComponentScan
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
