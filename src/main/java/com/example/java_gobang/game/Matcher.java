package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.packaging.mime.util.QEncoderStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

// 这个类表示匹配器,通过这个类负责完成整个匹配功能
@Component
public class Matcher {

    @Autowired
    private OnlineUserManager onlineUserManager;

    private ObjectMapper objectMapper;

    // 创建三个匹配队列
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    // 操作匹配队列的方法
    // 把玩家放到匹配队列中去
    public void add(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                normalQueue.offer(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "加入到了 normalQueue 中");
        }else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.offer(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "加入到了 highQueue 中");
        }else {
            synchronized (veryHighQueue) {
                veryHighQueue.offer(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "加入到了 veryHighQueue 中");
        }
    }

    // 当玩家停止匹配的时候,就需要将玩家从匹配队列中移除
    public void remove(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                normalQueue.remove(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "从 normalQueue 中移除了");
        }else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.remove(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "从 highQueue 中移除了");
        }else {
            synchronized (veryHighQueue) {
                veryHighQueue.remove(user);
            }
            System.out.println("把玩家: " + user.getUsername() + "从 veryHighQueue 中移除了");
        }
    }

    public Matcher() {
        // 创建三个线程,分别针对这三个匹配队列,进行操作
        Thread t1 = new Thread() {
            @Override
            public void run() {
                // 扫描 normalQueue
                while (true) {
                    handlerMatch(normalQueue);
                }
            }
        };
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(highQueue);
                }
            }
        };
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(veryHighQueue);
                }
            }
        };
        t3.start();
    }

    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue) {
            try {
                // 1. 检测队列中人数是否达到 2
                // 如果队列中人数不足,直接退出,等待下一轮扫描
                if (matchQueue.size() < 2) {
                    return;
                }
                // 2. 尝试从队列中取出两个玩家
                User player1 = matchQueue.poll();
                User player2 = matchQueue.poll();
                System.out.println("匹配出两个玩家: " + player1.getUsername() + ", " + player2.getUsername());

                // 3. 获取到玩家的 websocket 会话
                // 获取到会话的目的是告诉玩家,你排到了
                WebSocketSession session1 = onlineUserManager.getFromGameHall(player1.getUserId());
                WebSocketSession session2 = onlineUserManager.getFromGameHall(player2.getUserId());

                // 理论上 session 不会是 null
                // 因为前面的逻辑进行了处理,当玩家断开连接的时候就把玩家从匹配队列中移除了
                // 但是最好再进行一次判定
                if (session1 == null) {
                    // 如果玩家1 现在不在线了,就把玩家2 重新放回到匹配队列中
                    matchQueue.offer(player2);
                    return;
                }
                if (session2 == null) {
                    // 如果玩家2 现在不在线了,就把玩家1 重新放回到匹配队列中
                    matchQueue.offer(player1);
                    return;
                }

                // 当两个玩家排到是同一个用户的情况,一个玩家入队列了两次?
                // 理论上也是不存在的
                // 1)如果玩家下线,就会对玩家移除匹配队列
                // 2)又禁止了玩家多开
                if (session1 == session2) {
                    // 把其中一个玩家重新放回队列
                    matchQueue.offer(player1);
                    return;
                }

                // 4. TODO 把这两个玩家放到同一个游戏房间中

                // 5. 给玩家反馈信息,你匹配到对手了
                // 通过 websocket 返回一个 message 为 "matchSuccess" 这样的响应
                // 此处是需要给两个玩家返回信息,所以要返回两次
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                session1.sendMessage(new TextMessage(objectMapper.writeValueAsString(response1)));

                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                session2.sendMessage(new TextMessage(objectMapper.writeValueAsString(response2)));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
