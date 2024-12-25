package io.tinstafl.wikimedia;

import io.tinstafl.wikimedia.model.EventStreamsRequest;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PathBuilder {
  public static final String DEFAULT_PATH = "/recentchange";
  private final Logger log = LoggerFactory.getLogger(getClass());

  public String build(final EventStreamsRequest r) {
    var path = Optional.ofNullable(r)
      .map(request -> {
        var streams = Strings.join(request.streams(), ',');
        var p = streams.isBlank() ? DEFAULT_PATH : "/" + streams;
        return UriComponentsBuilder.fromPath(p)
          .queryParamIfPresent("last-event-id", Optional.ofNullable(request.lastEventIds()))
          .queryParamIfPresent("since", Optional.ofNullable(request.since()))
          .build()
          .toUriString();
      }).orElse(DEFAULT_PATH);

    log.info("created path for streams events request {}", path);
    return path;
  }
}
