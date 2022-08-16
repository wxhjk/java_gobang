package com.example.java_gobang.api;

import com.example.java_gobang.game.MatchRequest;
import com.example.java_gobang.game.MatchResponse;
import com.example.java_gobang.game.Matcher;
import com.example.java_gobang.game.OnlineUserManager;
import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// 通过这个类来处理匹配功能中的 websocket 请求
@Component
public class MatchAPI extends TextWebSocketHandler {

    private ObjectMapper objectMapper= new ObjectMapper();

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private Matcher matcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 玩家上线,把玩家加入到 OnlineUserManager
        // 1. 先获取到当前用户的身份(谁在游戏大厅中,建立的连接)
        //    此处的代码,之所以能够 getAttributes,全靠在注册 WebSocket 的时候
        //    加上的 .addInterceptors(new HttpSessionHandshakeInterceptor());
        //    这个逻辑就把 HttpSession 中的 Attribute 都给拿到 WebSocketSession 中了
        //    此时就可以在 WebSocketSession 中把之前的 HttpSession 里面存的 User 对象给拿到了

        // 这里的 user 可能为 null 所以要配制拦截器
        User user = (User) session.getAttributes().get("user");

        // 2. 先判断当前用户是否已经登录过(已经是在线状态的话,禁止多开),如果已经在线,就不该继续进行后续逻辑
        if (onlineUserManager.getFromGameHall(user.getUserId()) != null
                || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
            // 当前用户已经登录了!
            // 针对这个情况要告诉客户端,你这里重复登录了
            MatchResponse response = new MatchResponse();
            response.setOk(false);
            response.setReason("当前禁止多开");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            session.close();
            return;
        }
        // 3. 拿到了身份信息之后,就可以把玩家设置成在线状态了
        onlineUserManager.enterGameHall(user.getUserId(),session);
        System.out.println("玩家: " + user.getUsername() + " 进入游戏大厅!");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 实现处理开始匹配请求和处理停止匹配请求
        User user = (User) session.getAttributes().get("user");
        // 获取到客户端给服务器发送的数据
        String payload = message.getPayload();
        // 当前这个数据载荷是 json 格式的字符串,就需要把它转换成 Java 对象
        MatchRequest request = objectMapper.readValue(payload,MatchRequest.class);
        MatchResponse response = new MatchResponse();
        if (request.getMessage() .equals("startMatch")) {
            // 进入匹配队列
            matcher.add(user);
            response.setOk(true);
            response.setMessage("startMatch");
        }else if (request.getMessage().equals("stopMatch")) {
            // 退出匹配队列
            matcher.remove(user);
            // 移除之后,就可以返回一个响应给客户端了
            response.setOk(true);
            response.setMessage("stopMatch");
        }else {
            // 非法情况
            response.setOk(false);
            response.setReason("非法的匹配请求");
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 玩家下线,从 OnlineUserManager 中删除
        User user = (User) session.getAttributes().get("user");
        WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
        if (tmpSession == session) {
            onlineUserManager.exitGameHall(user.getUserId());
        }
        // 如果玩家在匹配中, 而 websocket 断开了,就应该移除匹配队列
        matcher.remove(user);

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 玩家下线,从 OnlineUserManager 中删除
        User user = (User) session.getAttributes().get("user");
        WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
        if (tmpSession == session) {
            onlineUserManager.exitGameHall(user.getUserId());
        }
        // 如果玩家在匹配中, 而 websocket 断开了,就应该移除匹配队列
        matcher.remove(user);
    }
}
