package com.yupi.usercentre.model.dto;

import com.yupi.usercentre.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 聊天消息查询封装类 --> 给前端返回的内容封装
 * 专门用来查询聊天信息
 *
 * 根据队伍Id查询聊天信息 -> 然后使用最后一条聊天消息用于分页加载聊天历史
 */
@EqualsAndHashCode(callSuper = true) // 继承父类的属性
@Data
public class ChatQuery extends PageRequest {

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 最后一条消息Id
     */
    private Long lastMessageId;


    // pageNum 和 pageSize 统一使用 PageRequest 类

    /**
     * 当前页码
     */
    // private Integer pageNum;

    /**
     * 每页显示的条数
     */
    // private Integer pageSize;

}
