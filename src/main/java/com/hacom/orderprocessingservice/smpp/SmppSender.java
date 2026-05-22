package com.hacom.orderprocessingservice.smpp;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class SmppSender {

    private final SmppSession session;

    public SmppSender(SmppSession session) {
        this.session = session;
    }

    public void sendTestMessage() {
        if (session == null) {
            log.error("❌ No SMPP session available. Cannot send message.");
            return;
        }

        try {
            SubmitSm submit = new SubmitSm();
            submit.setSourceAddress(new Address((byte)0x03, (byte)0x00, "1234"));
            submit.setDestAddress(new Address((byte)0x01, (byte)0x01, "04141234567"));
            submit.setShortMessage("Hola desde SMPP".getBytes());

            SubmitSmResp resp = session.submit(submit, 10000);

            log.info("📨 Message sent! SMPP Message ID: {}", resp.getMessageId());

        } catch (Exception e) {
            log.error("❌ Error sending SMPP message", e);
        }
    }
}
