package com.yupi.usercentre.model.WebSocket;

import com.google.gson.Gson;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.vo.ChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 聊天断点
 *
 * 链接地址：ws://localhost:8080/ws/chat/{teamId}
 * 例如：ws://localhost:8080/ws/chat/11
 */
@ServerEndpoint("/ws/chat/{teamId}") // 定义WebSocket的端点，{teamId} 是路径参数，会传递给方法
@Component
@Slf4j
public class ChatEndpoint {

    /**
     * 存储在线用户的 WebSocket 连接
     * 数据结构：Map<队伍ID, Set<WebSocket连接>>
     */
    private static final Map<Long, Set<Session>> teamSessions = new ConcurrentHashMap<>();

    /**
     * 存储每个Session对应的队伍Id
     * 数据结构：Map<SessionId, 队伍Id>
     */
    private static final Map<String, Long> sessionTeamMap = new ConcurrentHashMap<>();

    /**
     * JSON 工具 -> 使用Gson
     */
    private static final Gson gson = new Gson();


    /**
     * 建立连接时调用
     * @param session WebSocket会话
     * @param teamId 队伍Id(从URL路径中获取)
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("teamId") Long teamId) {
        // 1.将用户添加到队伍的在线列表
        /**
         * computerIfAbsent(key, mappingFunction)作用：如果teamSession中没有这个队伍id的键，就创建一个新的set；如果已经有了，返回现有的Set，然后把session添加到set中
         * teamId 作为键key
         * new CopyOnWriteArraySet<>() 作为value value，值不存在就创建新的Set，然后add到Session中
         */
        teamSessions.computeIfAbsent(teamId, k -> new CopyOnWriteArraySet<>())
                .add(session);

        // 2.记录这个Session对应的队伍Id（用于断开时查找）
        sessionTeamMap.put(session.getId(), teamId);

        // 3.打印日志
        log.info("用户连接成功，sessionId={}, teamId={}, 当前在线人数={}", session.getId(), teamId, teamSessions.get(teamId).size());
    }


    /**
     * 收到消息时调用
     *注意：通常不建议在这里处理业务逻辑！
     *
     * 推荐做法：
     * - 前端通过 HTTP 接口发送消息（POST /team/chat/send）
     * - 后端保存到数据库后，通过 WebSocket 推送给其他人
     * - WebSocket 只用于"推送"，不用于"接收业务数据"
     *
     * 为什么？
     * - HTTP 可以返回详细的错误信息（如参数错误、权限不足）
     * - WebSocket 的错误处理比较麻烦
     * - HTTP 接口更容易测试和调试
     * @param message 消息内容(字符串)
     * @param session WebSocket会话对象
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到消息：sessionId={}, message={}", session.getId(), message);

        // 如果需要在这里处理消息，可以解析JSON并调用Service
        // 但不推荐，建议使用HTTP接口
    }


    /**
     * 断开连接时调用
     *
     触发时机：
     * - 用户关闭浏览器标签页
     * - 用户刷新页面
     * - 网络断开
     * - 前端执行 ws.close()
     *
     * @param session WebSocket会话对象
     */
    @OnClose
    public void onClose(Session session) {
        // 1.获取这个session对应的队伍id - 从sessionTeamMap中查找这个连接属于哪个队伍
        Long teamId = sessionTeamMap.get(session.getId());

        if (teamId != null) { // 找到了这个队伍
            // 2.从队伍的在线列表中移除这个连接
            Set<Session> sessions = teamSessions.get(teamId);
            // todo 为什么两个if嵌套写？不是应该并列关系吗？if-else
            // 如果这个队伍中有在线用户 则移除这个连接
            if (sessions != null) {
                sessions.remove(session);

                // 3.如果这个队伍没有在线用户了，则从teamSessions中移除这个队伍
                if (sessions.isEmpty()) {
                    teamSessions.remove(teamId);
                }
            }

            // 4.清理Session映射，即队伍Id的映射
            sessionTeamMap.remove(session.getId());

            // 5.打印日志
            log.info("用户断开连接，sessionId={}, teamId={}", session.getId(),teamId);
        }
    }


    /**
     * 出错时调用
     * @param session WebSocket会话对象
     * @param throwable 错误信息
     */
    @OnError
    public void onError(Session session, Throwable throwable) {

        // 打印错误日志 -> session.getId() 获取哪个连接出错了， throwable 获取异常堆栈信息
        log.error("【WebSocket错误】, sessionId：{}，错误：{}", session.getId(), throwable);
    }


    /**
     * 广播消息给队伍所有成员
     *
     * 这个是WebSocket中最核心的方法
     * 需要在Service中调用这个方法推送消息
     *
     * @param teamId 队伍Id
     * @param chatVO 消息对象(会被转换成JSON发送)
     */
    public static void broadcastToTeam(Long teamId, ChatVO chatVO) {

        // 1.先获取这个队伍的所有在线连接
        Set<Session> sessions = teamSessions.get(teamId);

        // 2.检查是否有人在线
        if (sessions == null || sessions.isEmpty()) {
            log.info("【消息推送】队伍id：{}没有在线用户，消息已保存到数据库，其余用户上线后可以查看历史消息");
            // 用户上线后，可以调用getHistoryMessage接口查看历史消息获取这条信息
            // 因此就算没人在线，也可以不直接推送，直接返回即可
            return;
        }

        /*
         3.将消息对象转换为JSON字符串
           因为WebSocket只能发送字符串，所以需要将消息对象转换为JSON字符串
         */
        String jsonMessage = gson.toJson(chatVO);

        // 4.遍历所有在线用户，逐个推送，相对于给每个人在队伍聊天室都发送了(实际上只发一条消息，其余人都能接收到而已)
        int successCount = 0; // 成功推送的人数
        int failCount = 0; // 失败推送的人数

        // 遍历队伍的每个在线用户 -> 群发
        for (Session session : sessions) {
            try {
                // 检查连接是否有效
                if (session.isOpen()) {
                    // 连接有效，可以发消息 -> getBasicRemote():获取同步发送器，sendText():发送文本消息
                    session.getBasicRemote().sendText(jsonMessage);
                    successCount++; // 成功推送的人数加1
                } else {
                    // 连接已关闭，移除这个连接
                    sessions.remove(session);
                    failCount++;
                    throw new BusinessException(ErrorCode.NULL_ERROR,"【推送失败】连接已关闭");
                }

            } catch (Exception e) {
                failCount++;
                log.error("【消息推送失败】sessionId：{}，错误：{}", session.getId(), e.getMessage());
            }
        }

        // 5.打印推送结果
        log.info("【消息推送完成】队伍id：{}，在线用户：{}，推送成功：{}，推送失败：{}",
                teamId, sessions.size(), successCount, failCount);
    }


    /**
     * 获取队伍的在线人数
     * @param teamId
     * @return 在线人数
     */
    public static int getOnlineCount(Long teamId) {
        Set<Session> sessions = teamSessions.get(teamId);
        return sessions == null ? 0 : sessions.size();
    }
}
