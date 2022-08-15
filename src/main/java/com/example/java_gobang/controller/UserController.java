package com.example.java_gobang.controller;

import com.example.java_gobang.model.User;
import com.example.java_gobang.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

@RestController
@RequestMapping("/gobang")
public class UserController {

    @Autowired
    public UserService userService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @RequestMapping("/login")
    public HashMap<String, Object>  login (String username,String password) {
        HashMap<String,Object> result = new HashMap<>();
        result.put("success",200);
        String msg = "";
        int state = -1;
        // 进行非空校验 (前端和后端都需要进行非空检验)
        if (username == null || password == null || username.equals("") || password.equals("")) {
            msg = "用户名或密码为空";
            result.put("state",state);
            result.put("msg",msg);
            return result;
        }
        User user = userService.selectByName(username);
        if (user == null) {
            msg = "用户名不存在! 请先注册";
            result.put("state",state);
            result.put("msg",msg);
            return result;
        }
        if (user.getPassword().equals(password)) {
            // 登录成功
            state = 1;
            HttpSession session = request.getSession(true);
            session.setAttribute("user",user);
        }else {
            // 登录失败
            msg = "密码输入错误,请先检查";
        }
        result.put("state",state);
        result.put("msg",msg);
        return result;
    }

    @RequestMapping("/register")
    public HashMap<String, Object>  register(String username,String password) {
        HashMap<String,Object> result = new HashMap<>();
        result.put("success",200);
        String msg = "";
        int state = -1;
        // 进行非空校验 (前端和后端都需要进行非空检验)
        if (username != null && password != null && !username.equals("") && !password.equals("")) {
            User user = userService.selectByName(username);
            if (user != null) {
                // 注册失败, 用户名已经存在
                msg = "用户名已存在! 请重新注册";
                result.put("state",state);
                result.put("msg",msg);
                return result;
            }else {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setPassword(password);
                int ret = userService.add(newUser);
                state = 1;
            }
        }else {
            // 参数传递不全
            msg = "用户名或密码为空!";
        }
        // 3. 将执行结果返回给前端
        result.put("msg", msg);
        result.put("state", state);
        return result;
    }

    @GetMapping("/userInfo")
    public Object getUserInfo() {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        return user;
    }
}
