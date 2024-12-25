package io.tinstafl.wikimedia;

import io.tinstafl.kafka.model.KafkaConf;
import io.tinstafl.kafka.model.KafkaTopic;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class WikimediaKafkaConf {

  @Bean
  public KafkaAdmin kafkaAdmin(KafkaConf conf) {
    return new KafkaAdmin(kafkaAuthConf(conf));
  }

  @Bean
  public KafkaTemplate<Integer, String> kafkaTemplate(KafkaConf conf) {
    var kafkaConf = kafkaAuthConf(conf);

    kafkaConf.putAll(Map.of(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class,
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));

    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaConf));
  }

  @Bean
  public NewTopic streamEventsTopic(KafkaConf conf) {
    return TopicBuilder
      .name(KafkaTopic.WIKIMEDIA_STREAMS.value())
      .partitions(conf.admin().properties().partitions())
      .replicas(conf.admin().properties().replicas())
      .build();
  }

  private Map<String, Object> kafkaAuthConf(KafkaConf conf) {
    var kafkaConf = new HashMap<>(Map.of(
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, conf.bootstrapServers(),
      AdminClientConfig.SECURITY_PROTOCOL_CONFIG, conf.admin().security().protocol(),
      SaslConfigs.SASL_MECHANISM, conf.admin().properties().saslMechanism(),
      SaslConfigs.SASL_JAAS_CONFIG, conf.admin().properties().jaasConfig()));

    if (!conf.admin().properties().callbackHandler().isEmpty())
      kafkaConf.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, conf.admin().properties().callbackHandler());

    return kafkaConf;
  }
}
