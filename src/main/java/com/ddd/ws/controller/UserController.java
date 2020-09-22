package com.ddd.ws.controller;

import com.ddd.ws.handler.MarcoHandler;
import com.ddd.ws.message.Shout;
import com.ddd.ws.message.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.*;

@RestController
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private SimpMessageSendingOperations messaging;

    @Resource
    protected StringRedisTemplate  jedisStringTemplate;

    static private Map<String, String> users = new HashMap();
    static private Map<String, String> sessions = new HashMap();

    @GetMapping(path = "/getUsers")
    public Map<String, Object> getUsers() {

        return MarcoHandler.users;

    }

    @GetMapping(path = "/getAllUsers")
    public Map<String, String> getAllUsers() {
        logger.info("getAllUsers User:{}", users);
        return users;
    }

    @PostMapping(path = "/loginUser")
    public Map<String, Object> loginUser(HttpSession session, @RequestBody Map user) {
        Map<String, Object> rv = new HashMap<>();

        logger.info("login User:{} with sessionId:{}", user, session.getId());

        session.setAttribute("userInfo", user);

        ValueOperations<String, String> value = jedisStringTemplate.opsForValue();
        value.set(user.get("user").toString(), session.getId());
        value.set(session.getId(), user.get("user").toString());

        users.put(user.get("user").toString(), session.getId());
        sessions.put(session.getId(), user.get("user").toString());

        rv.put("code", "0000");
        rv.put("msg", "登录成功");
        rv.put("sid", session.getId());

        return rv;
    }

    @MessageMapping("/marco")
    //@SendTo("/topic/chat-room")
    @SendToUser("/queue/notifications")
    public Shout handleShout(Shout incoming, SimpMessageHeaderAccessor headerAccessor) {
        logger.info("Received message:{}", incoming.getMessage());

        String sessionId = headerAccessor.getSessionAttributes().get("sessionId").toString();
        System.out.println(sessionId);
        headerAccessor.setSessionId(sessionId);
        Principal a = new Principal() {
            @Override
            public String getName() {
                return "dd1";
            }
        };
        headerAccessor.setUser(a);

        Shout outgoing = new Shout();
        outgoing.setMessage(incoming.getMessage());

        ValueOperations<String, String> value = jedisStringTemplate.opsForValue();

        outgoing.setFrom(value.get(sessionId));
        outgoing.setTo(incoming.getTo());

        if (null != incoming.getTo()) {
            SimpMessageHeaderAccessor headerAccessor2 = SimpMessageHeaderAccessor
                    .create(SimpMessageType.MESSAGE);
            headerAccessor2.setSessionId(sessionId);
            headerAccessor2.setLeaveMutable(true);

            //messaging.convertAndSendToUser(sessionId, "/queue/notifications",
            //        outgoing, headerAccessor2.getMessageHeaders());
            messaging.convertAndSend("/queue/" + value.get(incoming.getTo()), outgoing);

            logger.info("Received message to:{}", incoming.getTo());
        }

        return outgoing;
    }

    @MessageMapping("/signal/{room}")
    public void signal(Signal incoming, SimpMessageHeaderAccessor headerAccessor, @DestinationVariable String room) {
        logger.info("Received message:{}", incoming.getMessage());

        String sessionId = headerAccessor.getSessionAttributes().get("sessionId").toString();
        System.out.println(sessionId);
        headerAccessor.setSessionId(sessionId);

        ValueOperations<String, String> value = jedisStringTemplate.opsForValue();

        incoming.setFrom(sessionId);

        messaging.convertAndSend("/topic/" + room, incoming);

    }

//
//    @SubscribeMapping({"/marco"})
//    public Shout handleSubscription() {
//        Shout outgoing = new Shout();
//        outgoing.setMessage("Polo! handleSubscription, DDD");
//        return outgoing;
//    }
}
