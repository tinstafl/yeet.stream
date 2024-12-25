package io.tinstafl.wikimedia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WikimediaRestConf {

  @Bean
  public RestClient wikimediaRestClient(WikimediaRestProperties conf) {
    return RestClient.builder()
      .requestFactory(new JdkClientHttpRequestFactory())
      .baseUrl(conf.host())
      .defaultHeaders((headers) -> {
        headers.add("Accept", conf.accept());
        headers.add("Accept-Encoding", conf.acceptEncoding());
        headers.add("Api-User-Agent", conf.apiUserAgent());
      })
      .build();
  }
}
