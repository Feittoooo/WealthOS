package com.wealthos.system.debug;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps the last log lines in memory so the debug page can show them without opening the
 * console. Hooks itself into Logback's root logger at startup.
 */
@Component
@ConditionalOnProperty(name = "wealthos.debug.enabled", havingValue = "true", matchIfMissing = true)
public class LogBuffer {

    private static final int CAPACITY = 1000;

    public record Entry(long id, Instant time, String level, String logger, String thread,
                        String requestId, String message, String stackTrace) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);
    private final AtomicLong sequence = new AtomicLong();
    private Appender appender;

    @PostConstruct
    void install() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender = new Appender();
        appender.setContext(context);
        appender.setName("debug-buffer");
        appender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
    }

    @PreDestroy
    void uninstall() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
        appender.stop();
    }

    public synchronized List<Entry> since(long afterId, String minLevel) {
        Level min = Level.toLevel(minLevel, Level.TRACE);
        return entries.stream()
                .filter(e -> e.id() > afterId)
                .filter(e -> Level.toLevel(e.level()).isGreaterOrEqual(min))
                .toList();
    }

    public synchronized void clear() {
        entries.clear();
    }

    private synchronized void add(ILoggingEvent event) {
        if (entries.size() == CAPACITY) entries.removeFirst();
        String stack = event.getThrowableProxy() == null ? null : ThrowableProxyUtil.asString(event.getThrowableProxy());
        entries.addLast(new Entry(sequence.incrementAndGet(), Instant.ofEpochMilli(event.getTimeStamp()),
                event.getLevel().toString(), event.getLoggerName(), event.getThreadName(),
                event.getMDCPropertyMap().get(RequestTraceFilter.MDC_KEY), event.getFormattedMessage(), stack));
    }

    private class Appender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            add(event);
        }
    }
}
