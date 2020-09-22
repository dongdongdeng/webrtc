package com.ddd.ws.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MarcoHandler extends AbstractWebSocketHandler {
    private static final Logger logger =
            LoggerFactory.getLogger(MarcoHandler.class);

    public static Map<String, Object> users = new HashMap<>();

    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message: {}, from session：{}", message.getPayload(), session.getId());

        Thread.sleep(2000);

        users.putIfAbsent(session.getId(), System.currentTimeMillis());

        //session.sendMessage(new TextMessage("Polo!" + "You are "+ session.getId()));

        session.sendMessage(new TextMessage("CONNECTED\n" +
                "server:RabbitMQ/3.8.5\n" +
                "session:session-sLxkrJfvarik8u8yM0BvDA\n" +
                "heart-beat:2000,2000\n" +
                "version:1.1\n" +
                "\n" + "\0"
                ));


    }

    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        logger.info("Received BinaryMessage: {}, from session：{}", message.getPayload(), session.getId());
    }

    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.info("Received PongMessage: {}, from session：{}", message.getPayload(), session.getId());
    }
}