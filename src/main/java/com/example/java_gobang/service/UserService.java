package com.example.java_gobang.service;

import com.example.java_gobang.mapper.UserMapper;
import com.example.java_gobang.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    public UserMapper userMapper;

    public int add(User user) {
        return userMapper.addUser(user);
    }

    public User selectByName(String username) {
        return userMapper.selectByName(username);
    }

    public void userWin(int userId) {
        userMapper.userWin(userId);
    }

    public void userLose(int userId) {
        userMapper.userLose(userId);
    }
}
