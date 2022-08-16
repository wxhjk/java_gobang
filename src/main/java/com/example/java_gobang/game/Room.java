package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import lombok.Data;

import java.util.UUID;

// 这个类就表示一个游戏房间
@Data
public class Room {
    // 使用字符串类型来表示,方便生成唯一的值
    private String roomId;

    private User user1;
    private User user2;

    // 先手方的玩家 id
    private int whiteUser;

    public Room() {
        // 构造 Room 的时候生成一个唯一的字符串表示房间ID
        // 使用 UUID 来作为房间ID
        roomId = UUID.randomUUID().toString();
    }
}
