package com.hacom.orderprocessingservice.smpp;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class SmppClient {

    private final SmppSession smppSession;

    // Constructor manual para permitir sesión opcional
    public SmppClient(@Autowired(required = false) SmppSession smppSession) {
        this.smppSession = smppSession;
        if (smppSession == null) {
            log.warn("SmppClient iniciado SIN sesión SMPP activa — SMS deshabilitado");
        } else {
            log.info("SmppClient iniciado con sesión SMPP activa");
        }
    }

    public void sendSms(String destination, String text) {
        if (smppSession == null || !smppSession.isBound()) {
            log.error("Cannot send SMS to {} — SMPP session is not connected", destination);
            return;
        }
        try {
            SubmitSm submit = new SubmitSm();
            submit.setSourceAddress(new Address((byte) 0x03, (byte) 0x00, "HACOM"));
            submit.setDestAddress(new Address((byte) 0x01, (byte) 0x01, destination));
            submit.setShortMessage(CharsetUtil.encode(text, CharsetUtil.CHARSET_GSM));
            log.info("Sending SMS to {} text: {}", destination, text);
            SubmitSmResp resp = smppSession.submit(submit, 30000);
            log.info("SMS sent | messageId: {}", resp.getMessageId());
        } catch (Exception e) {
            log.error("Error sending SMS to {}", destination, e);
        }
    }

    public void sendOrderProcessedSms(String phone, String orderId) {
        sendSms(phone, "Your order " + orderId + " has been processed");
    }
}