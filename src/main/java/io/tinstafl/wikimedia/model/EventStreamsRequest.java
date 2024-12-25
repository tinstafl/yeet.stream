package io.tinstafl.wikimedia.model;

import java.util.List;

public record EventStreamsRequest(
  List<String> streams,
  List<LastEventId> lastEventIds,
  String since
) {}
