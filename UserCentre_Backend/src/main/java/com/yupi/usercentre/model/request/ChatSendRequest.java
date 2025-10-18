package com.yupi.usercentre.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 发送聊天信息的请求体
 *
 * 需求：
 *     1. 发送消息
 *     2. 在同一个队伍才可以互相发送
 *     3. 不能发送给自己
 *     4. 发送的内容，系统识别后告诉对方消息类型
 */
@Data
public class ChatSendRequest  implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 发送人Id
     */
    private Long senderId;

    /**
     * 队伍Id
     */
    private Long teamId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 描述 -> 描述消息类型
     */
    private String description;
}
