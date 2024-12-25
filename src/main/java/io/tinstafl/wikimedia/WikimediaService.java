package io.tinstafl.wikimedia;

import io.tinstafl.kafka.EventStreamConsumer;
import io.tinstafl.kafka.KafkaProducer;
import io.tinstafl.kafka.model.KafkaTopic;
import io.tinstafl.wikimedia.model.EventStreamsRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Service
@AllArgsConstructor
public class WikimediaService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final PathBuilder pathBuilder;
  private final RestClient wikimediaRestClient;
  private final KafkaProducer kafkaProducer;

  public void collect(int count, KafkaTopic topic, EventStreamsRequest r) {
    log.info("emitting {} events to kafka topic {}", count, topic.value());

    new VirtualThreadTaskExecutor().execute(() -> {
      var path = pathBuilder.build(r);
      Objects.requireNonNull(
          wikimediaRestClient
            .get()
            .uri(path)
            .exchange((request, response) -> {
              log.info("wikimedia status code {}", response.getStatusCode());

              new EventStreamConsumer(kafkaProducer)
                .accept(response.getBody(), count, topic.value());

              return response;
            }, true))
        .close();
    });
  }
}
