package com.yupi.usercentre.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息包装类（脱敏）
 * VO 是 View Object 的简称，即返回给前端的数据对象
 *
 * 需求：显示消息Id，发送人Id，发送者用户信息，队伍Id，消息内容，消息类型，消息描述，发送时间
 * @author 17832
 */
@Data
public class ChatVO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    private Long messageId;

    /**
     * 发送人Id
     */
    private Long senderId;

    /**
     * 发送者信息(脱敏后)
     */
    private UserVO fromUserInfo;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 消息描述
     */
    private String description;

    /**
     * 发送时间
     */
    private Date createTime;

}
