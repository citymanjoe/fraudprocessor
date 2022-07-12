package com.telcoilng.fraudprocessor.processors.unifiedpayment;

import lombok.extern.slf4j.Slf4j;
import org.jpos.q2.Q2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class ConnectionConfig implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        log.info("Start XML configuration in deploy path");
        //todo extract deploy path to constant
        Q2 q2 = new Q2("deploy/cfg");
        q2.start();
    }


    @PreDestroy
    void StopService(){
        Q2 runningServer = Q2.getQ2();
        runningServer.shutdown();
    }
}
