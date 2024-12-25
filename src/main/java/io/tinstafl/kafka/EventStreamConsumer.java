package io.tinstafl.kafka;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.function.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class EventStreamConsumer implements TriConsumer<InputStream, Integer, String> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final KafkaProducer kafkaProducer;

  @Override
  public void accept(InputStream stream, Integer messageCount, String topic) {
    if (stream == null || messageCount == 0)
      return;

    String data;
    var totalMessageCount = 0;
    var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

    while (true) {
      try {
        if ((data = reader.readLine()) == null) break;
        if (totalMessageCount++ > messageCount) break;

        Thread.sleep(1000L);
      } catch (IOException | InterruptedException e) {
        log.error("error consuming input stream: {}", e.getMessage());
        throw new RuntimeException(e);
      }

      if (data.trim().isEmpty()) continue;

      log.info(data);
      kafkaProducer.send(topic, data);
    }
  }
}
