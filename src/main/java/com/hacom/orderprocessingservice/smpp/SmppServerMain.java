package com.hacom.orderprocessingservice.smpp;

import org.jsmpp.session.BindRequest;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmppServerMain {

    private static final Logger log = LoggerFactory.getLogger(SmppServerMain.class);
    private static final int PORT = 2775;

    public static void main(String[] args) throws Exception {
        log.info("Iniciando SMPP Server en puerto {}...", PORT);

        SMPPServerSessionListener listener = new SMPPServerSessionListener(PORT);
        ExecutorService executor = Executors.newCachedThreadPool();

        log.info("SMPP Server listo en puerto {}. Esperando conexiones...", PORT);

        while (true) {
            SMPPServerSession session = listener.accept();
            log.info("Nueva conexion aceptada: {}", session.getSessionId());
            executor.submit(() -> manejarSesion(session));
        }
    }

    private static void manejarSesion(SMPPServerSession session) {
        try {
            BindRequest bindRequest = session.waitForBind(30_000);

            String systemId = bindRequest.getSystemId();
            String password = bindRequest.getPassword();

            if ("admin".equals(systemId) && "admin".equals(password)) {
                log.info("Bind aceptado para systemId: {}", systemId);
                bindRequest.accept("SMPPServer");
                session.setEnquireLinkTimer(30_000);
                log.info("Sesion {} activa y lista", session.getSessionId());
            } else {
                log.warn("Bind rechazado: {}", systemId);
                bindRequest.reject(org.jsmpp.SMPPConstant.STAT_ESME_RBINDFAIL);
            }

        } catch (Exception e) {
            log.error("Error en sesion {}: {}", session.getSessionId(), e.getMessage());
        }
    }
}