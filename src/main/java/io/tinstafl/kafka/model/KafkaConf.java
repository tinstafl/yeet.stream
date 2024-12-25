package io.tinstafl.kafka.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaConf(
  List<String> bootstrapServers,
  Admin admin
) {}
