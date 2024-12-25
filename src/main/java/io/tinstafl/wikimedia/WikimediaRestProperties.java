package io.tinstafl.wikimedia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wikimedia")
public record WikimediaRestProperties(
  String host,
  String accept,
  String acceptEncoding,
  String apiUserAgent
) {}
