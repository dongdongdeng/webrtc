package com.ddd.ws.listener;

import com.ddd.ws.message.WebSocketChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component

public class WebSocketChatEventListener {

    private static Logger logger = LoggerFactory.getLogger(WebSocketChatEventListener.class);

    //@Autowired
    //private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {

        logger.info("Received a new web socket connection");

    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        logger.info("Received a web socket disconnection");

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if(username != null) {

            WebSocketChatMessage chatMessage = new WebSocketChatMessage();

            chatMessage.setType("Leave");

            chatMessage.setSender(username);

            //messagingTemplate.convertAndSend("/topic/public", chatMessage);

        }

    }

}
