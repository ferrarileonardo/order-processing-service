package com.hacom.orderprocessingservice.config;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.Behaviors;
import com.hacom.orderprocessingservice.akka.OrderActor;
import com.hacom.orderprocessingservice.metrics.OrderMetrics;
import com.hacom.orderprocessingservice.smpp.SmppClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Log4j2
@Configuration
public class ActorConfig {

    @Bean
    public ActorSystem<Void> actorSystem() {
        log.info("Initializing Akka ActorSystem");
        return ActorSystem.create(Behaviors.empty(), "order-processing-system");
    }

    @Bean
    public ActorRef<OrderActor.Command> orderActor(
            ActorSystem<Void> actorSystem,
            ReactiveMongoTemplate mongoTemplate,
            SmppClient smppClient,
            OrderMetrics orderMetrics
    ) {
        log.info("Creating OrderActor");
        return actorSystem.systemActorOf(
                OrderActor.create(mongoTemplate, smppClient, orderMetrics),
                "order-actor",
                Props.empty()
        );
    }
}