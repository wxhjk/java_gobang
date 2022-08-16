package com.example.java_gobang.api;

import com.example.java_gobang.game.GameReadyResponse;
import com.example.java_gobang.game.OnlineUserManager;
import com.example.java_gobang.game.Room;
import com.example.java_gobang.game.RoomManager;
import com.example.java_gobang.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.awt.font.GlyphMetrics;
import java.io.IOException;

@Component
public class GameAPI extends TextWebSocketHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        GameReadyResponse response = new GameReadyResponse();

        // 1. 先获取到用户的身份信息, (从 HttpSession 里拿到当前用户的对象)
        User user = (User) session.getAttributes().get("user");
        // 判断用户是否为 null ,也就是没有登录,但是配置了拦截器就不用考虑这个问题

        // 2. 判定当前用户是否已经进入房间了(拿着房间管理器进行查询)
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            // 如果为空表示该玩家还没有匹配到
            response.setOk(false);
            response.setReason("当前用户还未匹配到");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }

        // 3. 当前是否为多开,(当用户是否在其他地方已经进入游戏了)
        // 前面准备的 OnlineUserManager
        if (onlineUserManager.getFromGameHall(user.getUserId()) != null
                || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
            // 如果一个账号,一边是在游戏大厅,一边是在游戏房间,会视为多开
            response.setOk(false);
            response.setReason("禁止多开游戏页面");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            return;
        }

        // 4.设置当前玩家上线
        onlineUserManager.enterGameRoom(user.getUserId(),session);

        // 5. 把两个玩家加入到游戏房间中
        // 当前这个逻辑是在 game_hall.html 页面加载的时候进行的
        // 前面创建房间/匹配过程,是在 game_hall.html 页面中完成的
        // 因此前面匹配到对手之后,需要经过页面跳转,来到 game_room.html 才算正式的加入到了房间
        // 换句话说,执行到当前逻辑,说明玩家跳转页面已经成功了
        // 因为页面跳转,其实是个大活(很可能会出现 "失败" 的情况)
        synchronized (room) {
            if (room.getUser1() == null) {
                // 第一个玩家尚未加入到房间
                // 就把连上 websocket 的玩家作为玩家1 加入到房间
                room.setUser1(user);
                // 把先连入房间的玩家作为先手方
                room.setWhiteUser(user.getUserId());
                System.out.println("玩家: " + user.getUsername() + " 已经准备就绪! 作为玩家1");
                return;
            }

            if (room.getUser2() == null) {
                // 玩家1 已经进入到房间,当前玩家就为玩家2
                // 就把连上 websocket 的玩家作为玩家2 加入到房间
                room.setUser2(user);
                System.out.println("玩家: " + user.getUsername() + " 已经准备就绪! 作为玩家2");

                // 当两个玩家都加入成功之后,就要让服务器给这两个玩家都返回一个 websocket 的响应
                // 来通知双方说,游戏双方都已经准备好了
                // 通知玩家1
                noticeGameReady(room,room.getUser1(),room.getUser2());
                // 通知玩家2
                noticeGameReady(room,room.getUser2(),room.getUser1());
                return;
            }
        }

        // 6. 此时又有一个玩家尝试连接同一个房间,就提示报错\
        // 这种情况理论上是不存在的
        response.setOk(false);
        response.setReason("当前房间已满,您不能加入");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void noticeGameReady(Room room, User thisUser, User thatUser) throws IOException {
        GameReadyResponse resp = new GameReadyResponse();
        resp.setOk(true);
        resp.setMessage("gameReady");
        resp.setReason("");
        resp.setRoomId(room.getRoomId());
        resp.setThisUserId(thisUser.getUserId());
        resp.setThatUserId(thatUser.getUserId());
        resp.setWhiteUser(room.getWhiteUser());

        // 把当前数据传回给玩家
        WebSocketSession webSocketSession = onlineUserManager.getFromGameRoom(thisUser.getUserId());
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            // 此处简单处理,在断开连接的时候就不给客户端返回响应了
            return;
        }
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        if (session == exitSession) {
            // 加上这个判定,目的是为了避免在多开的情况下,第二个用户退出连接动作
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        System.out.println("当前用户: " + user.getUsername() + " 游戏房间连接异常!");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            // 此处简单处理,在断开连接的时候就不给客户端返回响应了
            return;
        }
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        if (session == exitSession) {
            // 加上这个判定,目的是为了避免在多开的情况下,第二个用户退出连接动作
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        System.out.println("当前用户: " + user.getUsername() + " 离开游戏房间!");
    }
}
