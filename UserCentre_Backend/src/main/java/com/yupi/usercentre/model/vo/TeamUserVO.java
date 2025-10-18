package com.yupi.usercentre.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类 (要脱敏！)
 * VO是Vue Object，是前端展示用的数据对象
 *
 * 添加关于聊天的字段：最新的一条消息lastMessage, 未读消息数量unreadCount, 当前在线人数onlineCount
 * @author 17832
 */
@Data
public class TeamUserVO implements Serializable {

    /**
     * 类的序列号
     */
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id（队长 id）
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 列举已经加入队伍的成员
     */
    // List<UserVO> userList;

    /**
     * 创建人的用户信息
     */
    private UserVO createUser;


    /**
     * 加入的用户id
     */
    private Integer hasJoinNum;

    /**
     * 是否已加入队伍
     */
    private boolean hasJoin;

    /**
     * 最新的一条消息
     */
    private ChatVO lastMessage;

    /**
     * 队伍中未读消息数量
     */
    private Integer unreadCount;

    /**
     * 当前在线人数
     */
    private Integer onlineCount;
}
