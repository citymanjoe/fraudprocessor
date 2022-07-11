package com.telcoilng.fraudprocessor.server;

import com.telcoilng.fraudprocessor.listener.ISOIncomingListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.PostChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.Properties;

@Component
@Slf4j
@RequiredArgsConstructor
public class Server implements CommandLineRunner {
    private final ResourceLoader resourceLoader;
    private final ApplicationContext applicationContext;
    private final ISOIncomingListener incomingListener;

    @Value("${isoserver.port}")
    int port;

    @Value("${keystorePath}")
    String keyStorePath;

    @Value("${keyStorePassword}")
    String keyStorePassword;

    ISOServer server;
    Logger logger = new Logger();


    @Override
    public void run(String... args) throws Exception {
        logger.addListener(new SimpleLogListener(System.out));
        //system monitor here
        new SystemMonitor(600000, logger, "system-monitor");

        //set server channel config for incoming message
        ServerChannel serverChannel = new PostChannel(new GenericPackager(resourceLoader.getResource("classpath:nibss.xml").getInputStream()));
        ((LogSource)serverChannel).setLogger (logger, "incoming server");

        //make the server SSL compatible
        //SocketFactory sslSocketFactory = new SocketFactory();
        //sslSocketFactory.setKeyStore(keyStorePath);
        //sslSocketFactory.setKeyPassword(keyStorePassword);
        //sslSocketFactory.setPassword(keyStorePassword);

        server = new ISOServer(port,serverChannel,null);
        server.setLogger (logger, "server");

        applicationContext.getAutowireCapableBeanFactory().autowireBean(incomingListener);

        //configure properties for server to kill channels when shutdown
        Properties properties = new Properties();
        properties.put("keep-channels",false);
        SimpleConfiguration configuration = new SimpleConfiguration(properties);

        //set server configurations
        server.setConfiguration(configuration);
        server.addISORequestListener(incomingListener);
//        server.setSocketFactory(sslSocketFactory);

        log.info("===============*Starting server*===============");
        log.info("===============*Port "+port+" *===============");
        new Thread (server).start ();

    }


    @PreDestroy
    public void shutdownServer() {
        log.info("===============*Shutting down server*===============");
        if (Objects.nonNull(server))
            server.shutdown();


    }
}
