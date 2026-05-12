package com.pd.rankingserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RankingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankingServerApplication.class, args);
    }

}
