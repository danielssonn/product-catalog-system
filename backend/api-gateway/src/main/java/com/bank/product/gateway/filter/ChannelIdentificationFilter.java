package com.bank.product.gateway.filter;

import com.bank.product.gateway.model.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Identifies and validates the channel for incoming requests
 */
@Slf4j
@Component
public class ChannelIdentificationFilter extends AbstractGatewayFilterFactory<ChannelIdentificationFilter.Config> {

    public static final String CHANNEL_HEADER = "X-Channel";
    public static final String CHANNEL_ATTRIBUTE = "gateway.channel";

    public ChannelIdentificationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Channel channel;
            
            // First check if channel is specified in config (route-based)
            if (config.getChannel() != null && !config.getChannel().isEmpty()) {
                channel = Channel.valueOf(config.getChannel());
            } else {
                // Otherwise, check header
                String channelHeader = exchange.getRequest().getHeaders().getFirst(CHANNEL_HEADER);
                channel = channelHeader != null ? 
                    Channel.valueOf(channelHeader) : 
                    Channel.PUBLIC_API; // Default to PUBLIC_API
            }
            
            // Store channel in exchange attributes
            exchange.getAttributes().put(CHANNEL_ATTRIBUTE, channel);
            
            // Add channel to response headers for debugging
            exchange.getResponse().getHeaders().add(CHANNEL_HEADER, channel.name());
            
            log.info("Request identified as channel: {}", channel);
            
            return chain.filter(exchange);
        };
    }

    @Data
    public static class Config {
        private String channel;

        public Config() {}

        public Config(String channel) {
            this.channel = channel;
        }
    }
}
