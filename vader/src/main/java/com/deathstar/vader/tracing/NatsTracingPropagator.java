package com.deathstar.vader.tracing;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collections;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class NatsTracingPropagator {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public NatsTracingPropagator(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("vader-nats", "1.0.0");
    }

    private static final TextMapSetter<Headers> SETTER =
            (headers, key, value) -> {
                if (headers != null) {
                    headers.put(key, value);
                }
            };

    private static final TextMapGetter<Message> GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Message message) {
                    if (message.getHeaders() == null) return Collections.emptyList();
                    return message.getHeaders().keySet();
                }

                @Override
                public String get(Message message, String key) {
                    if (message.getHeaders() == null) return null;
                    return message.getHeaders().getFirst(key);
                }
            };

    public void inject(Context context, Headers headers) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(context, headers, SETTER);
    }

    public Headers injectContext() {
        Headers headers = new Headers();
        inject(Context.current(), headers);
        return headers;
    }

    public Context extract(Context context, Message message) {
        return openTelemetry
                .getPropagators()
                .getTextMapPropagator()
                .extract(context, message, GETTER);
    }

    public void processMessageWithTracing(
            Message msg, String spanName, Consumer<Message> processor) {
        Context extractedContext = extract(Context.current(), msg);

        try (Scope scope = extractedContext.makeCurrent()) {
            Span span =
                    tracer.spanBuilder(spanName)
                            .setSpanKind(SpanKind.CONSUMER)
                            .setAttribute("messaging.system", "nats")
                            .setAttribute("messaging.destination", msg.getSubject())
                            .startSpan();

            try (Scope spanScope = span.makeCurrent()) {
                processor.accept(msg);
            } catch (Exception e) {
                span.recordException(e);
                throw e;
            } finally {
                span.end();
            }
        }
    }
}
