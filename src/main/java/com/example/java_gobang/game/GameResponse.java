package com.example.java_gobang.game;

import lombok.Data;

// 这个类表示落子的响应
@Data
public class GameResponse {
    private String message;
    private int userId;
    private int row;
    private int col;
    private int winner;
    
}
