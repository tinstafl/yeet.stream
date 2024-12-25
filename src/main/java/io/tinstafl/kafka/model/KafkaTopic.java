package io.tinstafl.kafka.model;

import lombok.Getter;

@Getter
public enum KafkaTopic {
  WIKIMEDIA_STREAMS("wikimedia.streams");

  private final String value;

  KafkaTopic(String value) {
    this.value = value;
  }
}
