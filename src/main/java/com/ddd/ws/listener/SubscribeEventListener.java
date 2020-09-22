package com.ddd.ws.listener;

import com.ddd.ws.config.WebSocketStompConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class SubscribeEventListener implements ApplicationListener {
    private static Logger logger = LoggerFactory.getLogger(SubscribeEventListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        SessionSubscribeEvent sessionSubscribeEvent = (SessionSubscribeEvent)applicationEvent;

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(sessionSubscribeEvent.getMessage());

        logger.info("onApplicationEvent:{}, {}", applicationEvent,
                headerAccessor.getSessionAttributes().get("sessionId").toString());
    }
}