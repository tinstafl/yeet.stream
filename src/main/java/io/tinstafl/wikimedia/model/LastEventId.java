package io.tinstafl.wikimedia.model;

public record LastEventId(
  String topic,
  int partition,
  int offset
) {}
