package com.yupi.usercentre.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercentre.model.domain.ChatMessage;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.request.ChatSendRequest;
import com.yupi.usercentre.model.vo.ChatVO;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
* @author 17832
* @description 针对表【chat_message(聊天消息表)】的数据库操作Service
* @createDate 2025-10-16 15:44:58
*/
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 获取队伍聊天室最新的一条消息
     * 应用场景：在队伍聊天页面，显示每个队伍最新消息预览
     * @param teamId 队伍id
     * @return 返回最新消息的VO对象，没有消息就返回null
     *
     * 业务流程：
     * 1.查询队伍的最新一条消息 -> 从chat_message表中查询
     * 2.查询发送者的用户信息 -> 从user表中查询
     * 3.组装成ChatVO对象返回
     */
    ChatVO getLastMessage(Long teamId);

    // 获取用户在队伍中查看最新消息的时间
    Date getUserLastReadTime(Long teamId, Long userId);

    // 更新用户查看队伍最新消息的时间
    void updateUserLastReadTime(Long teamId, Long userId);


    // 分页查询队伍聊天室历史消息
    Page<ChatVO> getHistoryMessage(Long teamId, Integer pageNum, Integer pageSize);

    // 用户在队伍聊天室中发送信息
    ChatVO sendMessage(ChatSendRequest chatSendRequest, User loginUser);
}
