package com.hacom.orderprocessingservice.smpp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SmppTestRunner implements CommandLineRunner {

    private final SmppSender sender;

    public SmppTestRunner(SmppSender sender) {
        this.sender = sender;
    }

    @Override
    public void run(String... args) {
        sender.sendTestMessage();
    }
}
