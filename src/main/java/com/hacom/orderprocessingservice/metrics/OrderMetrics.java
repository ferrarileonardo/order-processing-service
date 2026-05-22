package com.hacom.orderprocessingservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter ordersProcessed;

    public OrderMetrics(MeterRegistry registry) {
        this.ordersProcessed = registry.counter("orders_processed_total");
    }

    public void increment() {
        ordersProcessed.increment();
    }
}
