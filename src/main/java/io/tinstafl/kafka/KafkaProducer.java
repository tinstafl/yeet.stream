package io.tinstafl.kafka;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@AllArgsConstructor
public class KafkaProducer {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final KafkaTemplate<Integer, String> kafkaTemplate;

  public void send(final String topic, final String message) {
    log.info("producing kafka event {} for topic {}", message, topic);

    try {
      kafkaTemplate.send(topic, message).get(10L, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      log.error("error producing kafka event. moving on. {}", e.getMessage());
    }
  }
}
