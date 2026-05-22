package com.hacom.orderprocessingservice.smpp;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class SmppConfig {

    @Bean
    public SmppSession smppSession() {

        SmppSessionConfiguration config = new SmppSessionConfiguration();
        config.setName("hacom-smpp-client");
        config.setType(SmppBindType.TRANSCEIVER);

        //  Credenciales correctas para Diafaan/Ozeki
        config.setHost("127.0.0.1");
        config.setPort(2775);
        config.setSystemId("admin");
        config.setPassword("admin");

        DefaultSmppClient client = new DefaultSmppClient();

        log.info("📡 Connecting SMPP session to {}:{}", config.getHost(), config.getPort());

        try {
            return client.bind(config, new DefaultSmppSessionHandler());
        } catch (Exception e) {
            log.error(" SMPP bind failed, continuing without SMPP", e);
            return null; // No tumbar la app
        }
    }
}
