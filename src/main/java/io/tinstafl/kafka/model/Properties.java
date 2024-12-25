package io.tinstafl.kafka.model;

public record Properties(
  int partitions,
  int replicas,
  String saslMechanism,
  String jaasConfig,
  String callbackHandler
) {}
