package io.tinstafl;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class ApplicationPing {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final AttributeKey<String> ATTR_METHOD = AttributeKey.stringKey("method");

  private final Random random = new Random();
  private final Tracer tracer;
  private final LongHistogram pingHistogram;

  @Autowired
  public ApplicationPing(OpenTelemetry openTelemetry) {
    var meter = openTelemetry.getMeter(Application.class.getName());
    pingHistogram = meter.histogramBuilder("ping").ofLongs().build();
    tracer = openTelemetry.getTracer(Application.class.getName());
  }

  @GetMapping("/ping")
  public String ping() throws InterruptedException {
    var sleepTime = random.nextInt(200);

    ping(sleepTime);
    pingHistogram.record(sleepTime, Attributes.of(ATTR_METHOD, "ping"));

    return "pong";
  }

  private void ping(int sleepTime) throws InterruptedException {
    var span = tracer.spanBuilder("ping").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      Thread.sleep(sleepTime);
      log.info("pinging");
    } finally {
      span.end();
    }
  }
}
