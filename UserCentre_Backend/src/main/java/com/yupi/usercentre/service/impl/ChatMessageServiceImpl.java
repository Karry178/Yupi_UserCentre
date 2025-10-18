package com.yupi.usercentre.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercentre.model.WebSocket.ChatEndpoint;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.mapper.ChatMessageMapper;
import com.yupi.usercentre.mapper.UserMapper;
import com.yupi.usercentre.model.domain.ChatMessage;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.request.ChatSendRequest;
import com.yupi.usercentre.model.vo.ChatVO;
import com.yupi.usercentre.model.vo.UserVO;
import com.yupi.usercentre.service.ChatMessageService;
import com.yupi.usercentre.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author 17832
* @description 针对表【chat_message(聊天消息表)】的数据库操作Service实现
* @createDate 2025-10-16 15:44:58
*/
@Service
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService {

    // 注入聊天消息的mapper：chatMessageMapper
    @Resource
    private ChatMessageMapper chatMessageMapper;

    // 注入用户的mapper：userMapper，作用：查询放着的用户信息(头像，昵称等)
    @Resource
    private UserMapper userMapper;

    // 引入userTeamService
    @Resource
    private UserTeamService userTeamService;

    /**
     * 注入Redis Template, 为什么要加这两个泛型？ 规定
     * 泛型：K：key的类型，V：value的类型
     * key是String，value是Object
      */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 定义Redis Key的前缀常量
     * 完整格式：chat:last_read:用户ID:队伍ID        如chat:last_read:1001:2001
     * 作用是：记录用户最后查看的队伍消息时间
     */
    private static final String LAST_READ_TIME_KEY = "chat:last_read:";

    // 引入WebSocketHandler -> 实时聊天机制
    @Resource
    private ChatEndpoint chatEndpoint;


    /**
     * 获取队伍中最后一条消息
     * @param teamId 队伍id
     * @return 队伍中最后消息
     * @author 17832
     *
     * 说明：
     */
    @Override
    public ChatVO getLastMessage(Long teamId) {
        // 1.参数校验
        if (teamId == null || teamId <= 0) {
            log.error("获取最新消息失败：队伍id不合法，teamId={}",teamId);
            return null;
        }

        try {
            // 2.使用QueryWrapper查询最新一条消息
            QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("teamId",teamId) // 获取队伍id
                        .eq("isDelete",0) // 查询未删除的
                        .orderByDesc("createTime") // 按照创建时间倒序
                        .last("LIMIT 1"); // 限制只查一条

            // 执行查询 this.getOne()是serviceImpl提供的查询单条记录的方法
            ChatMessage lastMessage = this.getOne(queryWrapper);

            // 3.判断查询结果是否为空
            if (lastMessage == null) {
                log.info("队伍{}暂无消息",teamId);
                return null;
            }

            // 4.查询发送者消息 -> 返回给前端脱敏后的个人信息
            User fromUserInfo = userMapper.selectById(lastMessage.getSenderId());

            // 5.将查到的最新一条消息内容->设置给ChatVO,脱敏后返回给前端发送者信息
            ChatVO chatVO = new ChatVO();
            chatVO.setMessageId(lastMessage.getId()); // 设置消息id
            chatVO.setSenderId(lastMessage.getSenderId()); // 设置发送者id
            chatVO.setContent(lastMessage.getContent()); // 设置消息内容
            chatVO.setMessageType(lastMessage.getMessageType()); // 设置消息类型
            chatVO.setCreateTime(lastMessage.getCreateTime()); // 设置消息的发送时间

            // 设置发送者信息(脱敏后)
            if (fromUserInfo != null) {
                UserVO userVO = new UserVO();
                // 把查询到的用户信息设置给VO对象，通过copyproperties方法进行属性复制
                BeanUtils.copyProperties(fromUserInfo, userVO);
                chatVO.setFromUserInfo(userVO); // todo 这句代码是什么意思？
            }

            // 6.返回结果
            log.info("成功获取队伍{}的最新消息，messageId={}", teamId, lastMessage.getId());
            return chatVO;

        } catch (Exception e) {
            log.error("获取队伍id{}的最新消息失败",teamId, e);
            return null;
        }
    }


    // todo 实现用户查询在队伍中私聊窗口的最新消息


    /**
     * 辅助方法：获取用户在队伍中查看的最新消息时间
     * @param teamId
     * @param userId
     * @return
     * 逻辑：
     *      1.先查看Redis中是否存在
     *      2.没有的话，使用当前时间并存入Redis
     */
    @Override
    public Date getUserLastReadTime(Long teamId, Long userId) {
        // 先生成Redis Key
        String redisKey = LAST_READ_TIME_KEY + userId + ":" + teamId;
        // 从Redis获取时间
        Date lastReadTime = (Date) redisTemplate.opsForValue().get(redisKey);

        // 判断Redis中有无时间记录
        if (lastReadTime == null) {
            // 使用当前时间(把当前时间之前的消息都算已读)
            lastReadTime = new Date();
            // 存入Redis 7天
            redisTemplate.opsForValue().set(redisKey, lastReadTime, 7, TimeUnit.DAYS);
            log.info("用户{}首次查看队伍{},初始化最后查看时间", userId, teamId);
        }
        // 返回最后查看的时间
        return lastReadTime;
    }


    /**
     辅助方法：更新用户最后查看队伍的时间
     @param teamId
     @param userId
     应用场景：
         1.用户查看聊天历史时调用
         2.用户点击标记已读时调用
     */
    @Override
    public void updateUserLastReadTime(Long teamId, Long userId) {
        // 1.参数校验
        if (teamId == null || userId == null) {
            return;
        }

        try {
            // 1.生成Redis Key，作用是记录用户最后查看的队伍消息时间
            String redisKey = LAST_READ_TIME_KEY + userId + ":" + teamId;

            // 2.存储当前时间，设置过期时间7天
            Date now = new Date();
            redisTemplate.opsForValue().set(redisKey, now, 7, TimeUnit.DAYS);
            log.info("更新用户{}查看队伍{}的时间:{}", userId, teamId, now);
        } catch (Exception e) {
            // 更新失败不影响主流程，只记录日志
            log.error("更新用户{}查看队伍{}的时间失败", userId, teamId, e);
        }
    }


    /**
     * 获取队伍聊天室历史消息 -> 分页查询
     * @param teamId 队伍id
     * @param pageNum 当前页码
     * @param pageSize 每页显示数量
     * @return
     */
    @Override
    public Page<ChatVO> getHistoryMessage(Long teamId, Integer pageNum, Integer pageSize) {
        // 1.参数检验
        if (teamId == null || teamId <= 0) {
            log.error("您的队伍id不合法，请检查");
            // 返回空的page
            return new Page<>();
        }
        if (pageNum == null || pageNum <= 0) {
            log.error("您的页码参数不合法，请检查");
            return new Page<>();
        }
        if (pageSize == null || pageSize <= 0) {
            log.error("您的页码参数不合法，请检查");
            return new Page<>();
        }

        // 2.创建分页对象
        Page<ChatMessage> messagePage = new Page<>(pageNum, pageSize);

        // 3.创建查询条件
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId); // 筛选指定队伍
        queryWrapper.eq("isDelete",0); // 筛选未删除的消息
        queryWrapper.orderByDesc("createTime"); // 把消息按照发送时间倒序排序

        // 4.执行分页查询 -> 查到的数据 -> 封装成VO对象 -> 返回给前端
        Page<ChatMessage> chatMessagePage = this.page(messagePage, queryWrapper);

        // 5.※数据转换※
          // 5.1 创建一个新的分页对象，使用ChatVO对象返回给前端 列表
        Page<ChatVO> chatVOPage = new Page<>();
          // 5.2 从messagePage中获取列表 -> 遍历列表，对每条信息执行操作
          // 使用stream流
        // messagePage.getRecords().forEach(chatMessagePage -> {
        List<ChatVO> chatVOList = chatMessagePage.getRecords().stream().map(chatMessage -> {
            // 创建一个ChatVO对象
            ChatVO chatVO = new ChatVO();
            // 使用BeanUtils.copyProperties方法进行属性复制
            BeanUtils.copyProperties(chatMessage, chatVO);
            // 查询发送人的信息
            User senderInfo = userMapper.selectById(chatMessage.getSenderId());
            // 创建UserVO对象，脱敏后设置给ChatVO.fromUserInfo
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(senderInfo, userVO);
            // 将处理好的ChatVO添加到列表 -> 将脱敏后的userVO的发送人消息设置到chatVO中
            chatVO.setFromUserInfo(userVO);

            return chatVO;
        }).collect(Collectors.toList());

        // 6.封装返回结果
          // 6.1 创建一个分页ChatVO结果对象
        Page<ChatVO> newChatVOPage = new Page<>();
          // 6.2 设置总记录数
        newChatVOPage.setTotal(chatMessagePage.getTotal());
          // 6.3 设置当前页码
        newChatVOPage.setCurrent(chatMessagePage.getCurrent());
          // 6.4 获取每页页码数量
        newChatVOPage.setSize(chatMessagePage.getSize());
          // 6.5 必须设置数据列表！
        newChatVOPage.setRecords(chatVOList);

        log.info("获取队伍{}的信息成功",teamId);
        return newChatVOPage;
    }


    /**
     * 用户在队伍聊天室中发送信息
     *
     * @param chatSendRequest 发送消息请求
     * @param loginUser       登录用户
     * @return 发送消息结果
     * <p>
     * 业务流程：
     * 1.参数校验 -> 直接验证chatSendRequest请求不是空值
     * 2.查询队伍信息，保证用户在队伍中，非本队伍的人看不到消息 -> 使用teamId和userId查询
     * 3.创建消息对象，并设置消息各种属性 -> 把消息脱敏后设置给ChatVO对象
     * 4.创建发送人对象，脱敏后返给前端senderVO
     * 5.发送消息
     */
    @Override
    public ChatVO sendMessage(ChatSendRequest chatSendRequest, User loginUser) {
        // ========== 第一步：参数校验 ==========
        if (chatSendRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // ========== 第二步：权限校验（确保用户在队伍中）========== -> 要根据UserTeam查询
          /*
           2.1 先获取登录用户和发消息的人
           所有涉及"当前用户身份"的操作，必须从登录态（Session/Token）获取，绝不能信任前端传参！
           */
          // todo 如果两个都通过登录用户获取，岂不是两个是同一个人了？
        Long userId = loginUser.getId();
        Long senderId = loginUser.getId(); // senderId不应从请求体获取，前端可以伪造，冒充别人发消息
        Long teamId = chatSendRequest.getTeamId();
        // 2.2 根据teamId和userId查询
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        // queryWrapper.eq("senderId",senderId); 改为登录用户
        queryWrapper.eq("userId",userId);
          // 2.3 获取查询结果(聊天权限) -> 匹配后，可以发消息了
        /*ChatMessage chatAuth = this.getOne(queryWrapper);
        if (chatAuth == null) {
            throw new BusinessException(ErrorCode.NO_AUTH,"您不在该队伍中，无权发消息");
        }*/

        // 2.3 查找用户权限应该是判断是否加入了队伍(UserTeam)，而非是否发过消息(ChatMessage)
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH,"您不在该队伍中，无权发消息");
        }

        // ========== 第三步：构建消息对象并保存到数据库 ========== -> 发送
          // 3.1 设置消息属性 -> 队伍id，发送者id，接收者id，消息内容，消息类型，创建时间，更新时间，是否删除，消息状态
        ChatMessage chatMessage = new ChatMessage();
        // chatMessage.setId(0L); // 不要设置id，让Mybatis-Plus自动生成id
        chatMessage.setTeamId(chatSendRequest.getTeamId()); // 队伍id从登录用户获取
        chatMessage.setSenderId(loginUser.getId()); // 发送消息者的id通过请求参数获取
        chatMessage.setContent(chatSendRequest.getContent()); // 消息内容通过请求参数获取
        chatMessage.setMessageType(chatSendRequest.getMessageType());

        // chatMessage.setCreateTime(new Date()); 消息的发送时间和更新时间由数据库自动填充
        // chatMessage.setUpdateTime(new Date());
        // chatMessage.setIsDelete(0); 默认0

          // 3.2 保存到数据库（核心！） -> 走Service层，保存消息后发送消息
        boolean save = this.save(chatMessage);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"要发送的消息保存失败");
        }

        // ========== 第四步：构建返回的 ChatVO 对象 ==========
          //  否则就要发送消息了 -> 新建一个ChatVO对象，将chatMessage复制到sendMessageChatVO，设置脱敏后的数据发送
        ChatVO sendMessageChatVO = new ChatVO();
        // BeanUtils.copyProperties(chatMessage, sendMessageChatVO); 错误写法

        // 【重要错误改进】手动赋值ChatVO,因为ChatMessage的消息id是自增的，不可复制，且ChatVO中有自己的UserVO字段，所以要手动赋值
        sendMessageChatVO.setMessageId(chatMessage.getId()); // 消息id是 id -> messageId
        sendMessageChatVO.setSenderId(chatMessage.getSenderId());
        sendMessageChatVO.setContent(chatMessage.getContent());
        sendMessageChatVO.setMessageType(chatMessage.getMessageType());
        sendMessageChatVO.setCreateTime(chatMessage.getCreateTime());

            // 4的改进：先创建发送者对象，然后再赋值给新的UserVO对象; 因为如果用户不存在，原方法的selectById返回null，null无法复制到VO对象，报空指针异常
        User sender = userMapper.selectById(senderId);
          // 如果用户不等于空值，则进行赋值
        if (sender != null) {
            UserVO senderVO = new UserVO();
            BeanUtils.copyProperties(sender, senderVO);
            sendMessageChatVO.setFromUserInfo(senderVO);
        } else {
            throw new BusinessException(ErrorCode.NULL_ERROR,"发送者不存在");
        }

        // ========== 第五步：推送消息给队伍在线成员（核心！）==========
        // 发送消息 -> 是不是可以先检查Redis中是否有缓存，没有的话先存入Redis后再发送？ -> 不适合。发送消息属于写多读少的场景，每条消息都是新的，不会被重复读取
        // 加入WebSocket机制，广播发送的消息给队伍的所有成员 (类似于微信群聊)
        try {
            chatEndpoint.broadcastToTeam(teamId, sendMessageChatVO);
            // 解释：
            // - 如果有人在线，他们会立即收到消息
            // - 如果没人在线，消息已保存，他们上线后查询历史消息
        } catch (Exception e) {
            // 推送失败不会影响主流程
            log.info("消息推送失败，但是已经保存在数据库中");
        }
        return sendMessageChatVO;
    }


}




