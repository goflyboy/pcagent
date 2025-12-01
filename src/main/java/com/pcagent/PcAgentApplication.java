package com.pcagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 产品配置Agent应用主类
 */
@SpringBootApplication
@EnableAsync
public class PcAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PcAgentApplication.class, args);
    }
}

