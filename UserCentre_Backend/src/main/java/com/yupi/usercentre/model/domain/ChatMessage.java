package com.yupi.usercentre.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 聊天消息表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * 消息id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型(0-文本消息, 1-图片, 2-系统通知)
     */
    private Integer messageType;

    /**
     * 消息发送时间
     */
    private Date createTime;

    /**
     * 消息更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 序列化版本号
     * @TableField(exist = false): 表示该字段在数据库中不存在
     */
    @TableField(exist = false)
    private static final long SerialVersionUID = 1L;
}