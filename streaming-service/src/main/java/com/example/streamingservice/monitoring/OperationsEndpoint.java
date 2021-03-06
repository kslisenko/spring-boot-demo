package com.example.streamingservice.monitoring;

import com.example.streamingservice.StreamingApplication;
import com.example.streamingservice.event.QuoteEvent;
import com.example.streamingservice.event.QuoteReloadEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;

import static java.util.Collections.emptyList;

@Slf4j
@Endpoint(id = "operations")
@Service
public class OperationsEndpoint {

    @Autowired
    private StreamingApplication.ReactiveWebSocketHandler handler;

    @Autowired
    private ApplicationEventPublisher eventBus;


    @Autowired
    private MeterRegistry registry;

    private Counter updatesCounter;

    @ReadOperation
    public Set<String> getAllConnections() {
        return handler.getConnections().keySet();
    }

    /**
     * POST http://localhost:8080/actuator/operations/AAPL
     * {
     *     "symbol": "AAPL"
     * }
     */
    @WriteOperation
    public void flushData(@Selector String symbol) {
        log.info("flushing data to clients");
        eventBus.publishEvent(new QuoteReloadEvent(symbol));
    }

    @PostConstruct
    public void init() {
        registry.gauge("connections_count", emptyList(), handler, h -> h.getConnections().size());
        updatesCounter = registry.counter("quote_updates");
    }

    @EventListener
    public void onQuoteEvent(QuoteEvent e) {
        updatesCounter.increment();
    }
}