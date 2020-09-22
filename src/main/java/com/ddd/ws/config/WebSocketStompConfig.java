package com.ddd.ws.config;

import com.ddd.ws.controller.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {
    private static Logger logger = LoggerFactory.getLogger(WebSocketStompConfig.class);
    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletServerRequest = (ServletServerHttpRequest) request;
                    HttpServletRequest servletRequest = servletServerRequest.getServletRequest();
                    HttpSession session = servletRequest.getSession();
                    logger.info("beforeHandshake with sessionId:{}", session.getId());
                    attributes.put("sessionId", session.getId());
                    attributes.put("account", "dongdongdeng@qq.com");
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/marcopolo").withSockJS().setInterceptors(httpSessionHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        //registry.enableSimpleBroker("/queue", "/topic");

        //registry.setApplicationDestinationPrefixes("/app");

        registry.setApplicationDestinationPrefixes("/app")
                .setUserDestinationPrefix("/user")
                .enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest");

    }

    @Bean
    public ChannelInterceptor getMyInboundChannel() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                //logger.info("preSend Message Command:{}, message:{}", accessor.getCommand(), message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> attributes = (Map<String, Object>)message.getHeaders().get("simpSessionAttributes");
                    logger.info("getMyInboundChannel preSend CONNECT Message from sessionId:{}", attributes.get("sessionId"));
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    Map<String, Object> attributes = (Map<String, Object>)message.getHeaders().get("simpSessionAttributes");
                    logger.info("getMyInboundChannel preSend SEND Message from sessionId:{}", attributes.get("sessionId"));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

                    logger.info("getMyInboundChannel preSend " + accessor.getDestination());

                    if (accessor.getDestination().startsWith("/person")) {
                        //accessor.setDestination(accessor.getDestination().replaceFirst("/person", ""));
                    }

                    return message;
                }

                return message;
            }
        };
    }

    @Bean
    public  ChannelInterceptor getMyOutboundChannel() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.MESSAGE.equals(accessor.getCommand())) {

                    logger.info("getMyOutboundChannel preSend MESSAGE:{}", message);

                    //accessor.setDestination("/person" + accessor.getDestination());

                }

                return message;
            }
        };
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(getMyInboundChannel());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(getMyOutboundChannel());
    }
}
