package com.hacom.orderprocessingservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Date;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Log4j2
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ReactiveMongoTemplate mongoTemplate;

    @GetMapping("/{orderId}")
    public Mono<OrderDocument> getOrder(@PathVariable String orderId) {
        log.info("Getting order {}", orderId);
        return mongoTemplate.findOne(
                query(where("orderId").is(orderId)),
                OrderDocument.class,
                "orders"
        );
    }

    @GetMapping("/total")
    public Mono<Long> getTotalOrders(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        log.info("Getting total orders from {} to {}", from, to);

        Date fromDate = Date.from(from.toInstant());
        Date toDate = Date.from(to.toInstant());

        return mongoTemplate
                .query(OrderDocument.class)
                .inCollection("orders")
                .matching(query(where("ts").gte(fromDate).lte(toDate)))
                .count();
    }
}