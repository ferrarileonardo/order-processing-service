package com.hacom.orderprocessingservice.grpc;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.hacom.orderprocessingservice.akka.OrderActor;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

@Log4j2
@GrpcService
@RequiredArgsConstructor
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private final ActorRef<OrderActor.Command> orderActor;
    private final ActorSystem<Void> actorSystem;

    private boolean isValid(OrderRequest request) {
        if (request.getOrderId().isEmpty()) return false;
        if (request.getCustomerId().isEmpty()) return false;
        if (request.getCustomerPhoneNumber().isEmpty()) return false;
        if (request.getItemsList().isEmpty()) return false;
        for (OrderItem item : request.getItemsList()) {
            if (item.getSku().isEmpty()) return false;
            if (item.getQuantity() <= 0) return false;
        }
        return true;
    }

    @Override
    public void processOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        log.info("Received gRPC order: {}", request.getOrderId());

        if (!isValid(request)) {
            log.warn("Invalid order received: {}", request.getOrderId());
            responseObserver.onNext(
                    OrderResponse.newBuilder()
                            .setOrderId(request.getOrderId())
                            .setStatus("ERROR")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        CompletionStage<OrderActor.Response> result =
                AskPattern.ask(
                        orderActor,
                        replyTo -> new OrderActor.ProcessOrderCommand(request, replyTo),
                        Duration.ofSeconds(5),
                        actorSystem.scheduler()
                );

        result.whenComplete((reply, ex) -> {
            if (ex != null) {
                log.error("Error processing order {}", request.getOrderId(), ex);
                responseObserver.onError(ex);
                return;
            }

            log.info("Order {} processed with status {}", reply.orderId(), reply.status());
            responseObserver.onNext(
                    OrderResponse.newBuilder()
                            .setOrderId(reply.orderId())
                            .setStatus(reply.status())
                            .build()
            );
            responseObserver.onCompleted();
        });
    }
}