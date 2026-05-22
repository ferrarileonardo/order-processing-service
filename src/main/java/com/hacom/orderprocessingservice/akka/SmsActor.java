package com.hacom.orderprocessingservice.akka;
import com.hacom.orderprocessingservice.akka.OrderActor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SmsActor extends AbstractBehavior<SmsActor.Command> {

    // ======== MENSAJES ========
    public interface Command {}

    public record SendSmsCommand(
            String phoneNumber,
            String message,
            ActorRef<? super SmsSentResponse> replyTo
    ) implements Command {}

    // ⭐ Nuevo mensaje interno para reintentos
    public record RetrySmsCommand(
            String phoneNumber,
            String message,
            ActorRef<? super SmsSentResponse> replyTo,
            int attempt,
            int maxAttempts
    ) implements Command {}

    public record SmsSentResponse(boolean success) implements OrderActor.Command {}

    // ======== FACTORY ========
    public static Behavior<Command> create() {
        return Behaviors.setup(SmsActor::new);
    }

    private SmsActor(ActorContext<Command> context) {
        super(context);
    }

    // ======== RECEIVE ========
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SendSmsCommand.class, this::onSendSms)
                .onMessage(RetrySmsCommand.class, this::onRetrySms)
                .build();
    }

    // ======== LÓGICA PRINCIPAL ========
    private Behavior<Command> onSendSms(SendSmsCommand cmd) {

        log.info("📨 Sending SMS to {}: {}", cmd.phoneNumber(), cmd.message());

        // ⭐ 1. Simular fallo aleatorio
        if (Math.random() < 0.3) {
            log.error("❌ SMS failed (first attempt) to {}", cmd.phoneNumber());

            // ⭐ Enviar mensaje de reintento
            getContext().getSelf().tell(
                    new RetrySmsCommand(
                            cmd.phoneNumber(),
                            cmd.message(),
                            cmd.replyTo(),
                            1,          // attempt
                            3           // maxAttempts
                    )
            );

            return this;
        }

        // ⭐ 2. Simular latencia
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // ⭐ 3. Respuesta exitosa
        cmd.replyTo().tell(new SmsSentResponse(true));
        return this;
    }

    // ======== LÓGICA DE REINTENTOS ========
    private Behavior<Command> onRetrySms(RetrySmsCommand cmd) {

        log.warn("🔁 Retry attempt {} of {} for {}",
                cmd.attempt(), cmd.maxAttempts(), cmd.phoneNumber());

        // ⭐ Simular fallo nuevamente
        if (Math.random() < 0.3) {

            if (cmd.attempt() >= cmd.maxAttempts()) {
                log.error("❌ SMS permanently failed after {} attempts", cmd.maxAttempts());
                cmd.replyTo().tell(new SmsSentResponse(false));
                return this;
            }

            // ⭐ Enviar siguiente reintento
            getContext().getSelf().tell(
                    new RetrySmsCommand(
                            cmd.phoneNumber(),
                            cmd.message(),
                            cmd.replyTo(),
                            cmd.attempt() + 1,
                            cmd.maxAttempts()
                    )
            );

            return this;
        }

        // ⭐ Si el reintento funciona
        log.info("📨 SMS sent successfully on retry {}", cmd.attempt());
        cmd.replyTo().tell(new SmsSentResponse(true));
        return this;
    }
}
