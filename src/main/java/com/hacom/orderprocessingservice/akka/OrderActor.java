package com.hacom.orderprocessingservice.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.hacom.orderprocessingservice.api.OrderDocument;
import com.hacom.orderprocessingservice.grpc.OrderRequest;
import com.hacom.orderprocessingservice.metrics.OrderMetrics;
import com.hacom.orderprocessingservice.smpp.SmppClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class OrderActor extends AbstractBehavior<OrderActor.Command> {

    public interface Command {}

    public record ProcessOrderCommand(
            OrderRequest request,
            ActorRef<Response> replyTo
    ) implements Command {}

    public record Response(
            String orderId,
            String status
    ) {}

    private final ReactiveMongoTemplate mongoTemplate;
    private final SmppClient smppClient;
    private final OrderMetrics orderMetrics;

    public static Behavior<Command> create(
            ReactiveMongoTemplate mongoTemplate,
            SmppClient smppClient,
            OrderMetrics orderMetrics
    ) {
        return Behaviors.setup(ctx -> new OrderActor(ctx, mongoTemplate, smppClient, orderMetrics));
    }

    private OrderActor(
            ActorContext<Command> context,
            ReactiveMongoTemplate mongoTemplate,
            SmppClient smppClient,
            OrderMetrics orderMetrics
    ) {
        super(context);
        this.mongoTemplate = mongoTemplate;
        this.smppClient = smppClient;
        this.orderMetrics = orderMetrics;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessOrderCommand.class, this::onProcessOrder)
                .build();
    }

    private OrderDocument toDocument(OrderRequest req) {
        List<String> items = req.getItemsList()
                .stream()
                .map(item -> item.getSku() + " x" + item.getQuantity())
                .collect(Collectors.toList());

        OrderDocument doc = new OrderDocument();
        doc.setOrderId(req.getOrderId());
        doc.setCustomerId(req.getCustomerId());
        doc.setCustomerPhoneNumber(req.getCustomerPhoneNumber());
        doc.setStatus("PROCESSED");
        doc.setItems(items);
        doc.setTs(new Date());
        return doc;
    }

    private Behavior<Command> onProcessOrder(ProcessOrderCommand msg) {
        var order = msg.request();
        log.info("Actor received order {} for processing", order.getOrderId());

        OrderDocument document = toDocument(order);

        mongoTemplate
                .save(document, "orders")
                .doOnSuccess(saved ->
                        log.info("Order {} saved in MongoDB", order.getOrderId())
                )
                .doOnError(error ->
                        log.error("Error saving order {}: {}", order.getOrderId(), error.getMessage())
                )
                .then(Mono.fromRunnable(() ->
                        smppClient.sendOrderProcessedSms(
                                order.getCustomerPhoneNumber(),
                                order.getOrderId()
                        )
                ))
                .doOnSuccess(v -> {
                    orderMetrics.increment();
                    log.info("Metric incremented: orders_processed_total");
                })
                .subscribe();

        msg.replyTo().tell(new Response(order.getOrderId(), "PROCESSED"));
        return this;
    }
}