package com.example.java_gobang.mapper;

import com.example.java_gobang.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    // 插入一个用户,实现注册的功能
    int addUser(User user);

    User selectByName(String username);

    // 总比赛场数 + 1 获胜场数 + 1 天梯分数 + 100
    void userWin(int userId);

    // 总比赛场数 + 1 获胜场数 不变 天梯分数 - 100
    void userLose(int userId);
}
