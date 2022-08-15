package com.example.java_gobang.config;

import com.example.java_gobang.api.MatchAPI;
import com.example.java_gobang.api.TestAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TestAPI testAPI;

    @Autowired
    private MatchAPI matchAPI;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(testAPI,"/test");
        webSocketHandlerRegistry.addHandler(matchAPI,"/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
