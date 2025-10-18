/*
package com.yupi.usercentre.model.WebSocket;

import com.google.gson.Gson;
import com.yupi.usercentre.model.vo.ChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

*/
/**
 * 创建WebSocket处理器 -> 聊天消息处理器
 *
 * 作用：
 * 1. 处理客户端连接、断开事件
 * 2. 接收客户端发送的消息
 * 3. 向客户端推送消息
 * 4. 维护在线用户列表
 *//*

@Component // 让Spring管理这个Bean
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    */
/**
     * 存储在线用户的 WebSocket 连接
     *
     * 数据结构：Map<队伍ID, Set<WebSocket连接>>
     *
     * 为什么用 ConcurrentHashMap？
     * - 多个用户可能同时连接/断开
     * - 需要线程安全的 Map
     *
     * 为什么用 CopyOnWriteArraySet？
     * - 多个用户可能同时发送消息
     * - 需要线程安全的 Set
     * - 读多写少的场景（适合用 CopyOnWrite）
     *//*

    private final Map<Long, Set<WebSocketSession>> teamSessions = new ConcurrentHashMap<>();

    */
/**
     * 存储每个 WebSocket 连接对应的队伍ID
     *
     * 数据结构：Map<WebSocket连接ID, 队伍ID>
     *
     * 为什么需要？
     * - 用户断开连接时，需要知道他在哪个队伍
     * - 才能从对应队伍的在线列表中移除
     *//*

    private final Map<String, Long> sessionTeamMap = new ConcurrentHashMap<>();

    */
/**
     * Gson: JSON转换工具
     *//*

    private final Gson gson = new Gson();


    // 用户连接时
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        // 从session中获取用户id和队伍id
        // 1.从URL参数中获取队伍Id
        getQueryParam(session, "teamId");

        // 将用户添加到对应队伍的在线列表
    }

    // 用户断开连接时
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 从在线列表中移除用户
    }

    // 接收到消息时
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 解析消息内容

        // 调用Service保存信息

        // 广播给队伍其余成员
    }

    // 群发消息
    public void broadcastToTeam(Long teamId, ChatVO chatVO) {

        Set<WebSocketSession> sessions = teamSessions.get(teamId);

        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                session.sendMessage(new TextMessage(JSON.toJSONString(chatVO)));
            }
        }
    }


    private String getQueryParam(WebSocketSession session, String paramName) {
        // 1. 从URL参数中获取参数值
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }

        // 解析查询字符串
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
        }
    }
}
*/
