package io.tinstafl.wikimedia;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.tinstafl.Application;
import io.tinstafl.kafka.model.KafkaTopic;
import io.tinstafl.wikimedia.model.EventStreamsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/v1/wikimedia/emit")
public class WikimediaController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final AttributeKey<String> ATTR_METHOD = AttributeKey.stringKey("method");

  private final Tracer tracer;
  private final LongHistogram wikimediaHistogram;
  private final WikimediaService wikimediaService;

  public WikimediaController(OpenTelemetry otel, WikimediaService wikimediaService) {
    this.wikimediaService = wikimediaService;

    var meter = otel.getMeter(Application.class.getName());
    this.wikimediaHistogram = meter.histogramBuilder("wikimedia").ofLongs().build();
    this.tracer = otel.getTracer(Application.class.getName());
  }

  @PostMapping("/streams")
  public ResponseEntity<Void> getStreamsEvents(@RequestParam(defaultValue = "10") int total, @RequestBody(required = false) EventStreamsRequest request) {
    log.info("producing kafka wikimedia streams {}", request);

    var start = Instant.now();
    var span = tracer.spanBuilder("streaming-events").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      log.info("sending wikimedia streaming events to kafka topic");

      wikimediaService.collect(total, KafkaTopic.WIKIMEDIA_STREAMS, request);
      wikimediaHistogram.record(Duration.between(start, Instant.now()).toMillis(), Attributes.of(ATTR_METHOD, "streams"));
    } finally {
      span.end();
    }

    return ResponseEntity.ok().build();
  }
}
