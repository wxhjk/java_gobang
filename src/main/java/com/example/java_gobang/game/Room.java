package com.example.java_gobang.game;

import com.example.java_gobang.JavaGobangApplication;
import com.example.java_gobang.model.User;
import com.example.java_gobang.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
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


    // @Autowired
    private OnlineUserManager onlineUserManager;

    // 引入 roomManager 用于房间销毁
    // @Autowired
    private RoomManager roomManager;

    private UserService userService;

    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;


    private int[][] board = new int[MAX_ROW][MAX_COL];
    // 这个二维数组用来表示棋盘
    // 约定:
    // 1) 使用 0 来表示当前位置未落子,初始化好的 int 数组就相当于全 0
    // 2) 使用 1 来表示 user1 的落子位置
    // 3) 使用 2 来表示 user2 的落子位置

    // 创建 objectMapper 来转换 json
    private ObjectMapper objectMapper = new ObjectMapper();

    // 通过这个方法来处理一次落子的操作
    // 要做的事情:
    // 1.记录当前落子的位置
    // 2. 进行胜负判定
    // 3. 给客户端返回响应

    public void putChess(String jsonString) throws IOException {

        // 1. 记录当前落子的位置
        GameRequest request = objectMapper.readValue(jsonString,GameRequest.class);
        GameResponse response = new GameResponse();
        // 判断当前这个子是玩家1 落的还是玩家2 落的,决定往数组中加 1 还是 2
        int chess = request.getUserId() == user1.getUserId() ? 1 : 2;
        int row = request.getRow();
        int col = request.getCol();
        if (board[row][col] != 0) {
            // 在客户端已经针对重复落子进行过判定了,此处为了程序更加健壮,在服务器再判定一次
            System.out.println("当前位置 (" + row + ", " + col + " ) 已经有子了!");
            return;
        }
        // 2. 打印出当前的棋盘信息,用于方便观察局势,也便于后续关于胜负的判定

        board[row][col] = chess;

        printBoard();
        // 2. 进行胜负判定
        int winner = checkWinner(row,col,chess);
        // 3. 给 房间中的所有客户端!!! 都返回响应
        response.setMessage("putChess");
        response.setRow(row);
        response.setCol(col);
        response.setWinner(winner);
        response.setUserId(request.getUserId());

        WebSocketSession session1 = onlineUserManager.getFromGameRoom(user1.getUserId());
        WebSocketSession session2 = onlineUserManager.getFromGameRoom(user2.getUserId());

        // 万一查到的会话为空( 玩家直接下线了) 需要特殊处理一下
        if (session1 == null) {
            // 如果玩家1 下线了,就直接认为玩家2 获胜了
            response.setWinner(user2.getUserId());
            System.out.println("玩家1 掉线!");
        }
        if (session2 == null) {
            response.setWinner(user1.getUserId());
            System.out.println("玩家2 掉线!");
        }

        if (session1 != null) {
            session1.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
        if (session2 != null) {
            session2.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }

        // 4. 如果当前胜负已分,那么房间就失去存在的意义了,就需要把房间从房间管理器中移除
        if (response.getWinner() != 0) {

            // 更新获胜方和失败方的信息
            userService.userWin(response.getWinner());
            int userLoseId = (response.getWinner() == user1.getUserId()) ? user2.getUserId() : user1.getUserId();
            userService.userLose(userLoseId);

            // 胜负已分
            System.out.println("游戏结束! 房间即将销毁! roomId=" + roomId + " 获胜方为 " + response.getWinner());
            // 销毁房间
            roomManager.remove(roomId,user1.getUserId(),user2.getUserId());
        }
    }

    private void printBoard() {

        System.out.println("[打印棋盘信息]");
        System.out.println("=====================================================");
        for (int r = 0; r < MAX_ROW; r++) {
            for (int c = 0; c < MAX_COL; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("=====================================================");

    }

    private int checkWinner(int row, int col, int chess) {
        // 如果玩家1获胜,就返回玩家1的 userId
        // 如果玩家2获胜,就返回玩家2的 userId
        // 如果未分胜负,就返回 0

        // 1. 检查所有的行
        // 先遍历这 5 种情况
        for (int c = col - 4; c <= col; c++) {
            // 针对其中的一种情况,来判定这五个子是不是连在一起了
            // 不光是这五个子得连着,而且还必须和玩家连的子一样,才算是获胜
            try {
                if (board[row][c] == chess
                && board[row][c + 1] == chess
                && board[row][c + 2] == chess
                && board[row][c + 3] == chess
                && board[row][c + 4] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果数组出现越界的情况.就直接在这里忽略异常
                continue;
            }
        }

        // 2. 检查所有的列
        for (int r = row - 4; r <= row; r++) {
            try {
                if (board[r][col] == chess
                        && board[r + 1][col] == chess
                        && board[r + 2][col] == chess
                        && board[r + 3][col] == chess
                        && board[r + 4][col] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果数组出现越界的情况.就直接在这里忽略异常
                continue;
            }
        }

        // 3. 检查左对角线
        for (int r = row - 4, c = col - 4; r <= row && c <= col; r++,c++) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c + 1] == chess
                        && board[r + 2][c + 2] == chess
                        && board[r + 3][c + 3] == chess
                        && board[r + 4][c + 4] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果数组出现越界的情况.就直接在这里忽略异常
                continue;
            }
        }

        // 4. 检查右对角线
        for (int r = row - 4, c = col + 4; r <= row && c >= col; r++,c--) {
            try {
                if (board[r][c] == chess
                        && board[r + 1][c - 1] == chess
                        && board[r + 2][c - 2] == chess
                        && board[r + 3][c - 3] == chess
                        && board[r + 4][c - 4] == chess) {
                    return chess == 1 ? user1.getUserId() : user2.getUserId();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // 如果数组出现越界的情况.就直接在这里忽略异常
                continue;
            }
        }

        // 还用一种和棋的情况
        return 0;
    }

    public Room() {
        // 构造 Room 的时候生成一个唯一的字符串表示房间ID
        // 使用 UUID 来作为房间ID
        roomId = UUID.randomUUID().toString();

        // 通过入口记录的 context ,我们就可以手动获取到 OnlineUserManager 和 RoomManager
        onlineUserManager = JavaGobangApplication.context.getBean(OnlineUserManager.class);
        roomManager = JavaGobangApplication.context.getBean(RoomManager.class);
        userService = JavaGobangApplication.context.getBean(UserService.class);
    }
}
