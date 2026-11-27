package com.petCare.petCare.domain.realtime.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.petCare.petCare.domain.realtime.service.RedisFamilyEventBroker;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisPubSubConfig {

	@Bean
	public RedisMessageListenerContainer familyEventListenerContainer(
		RedisConnectionFactory connectionFactory,
		RedisFamilyEventBroker broker
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(broker, new PatternTopic(RedisFamilyEventBroker.CHANNEL_PREFIX + "*"));
		return container;
	}

}
