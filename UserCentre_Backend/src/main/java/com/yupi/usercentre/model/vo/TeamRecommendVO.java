package com.yupi.usercentre.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;


/**
 * 基于队伍中成员所在城市进行匹配，返回推荐的队伍详细信息 -> 发给前端(脱敏后的)
 */
@Data
public class TeamRecommendVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 当前人数
     */
    private Integer currentNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 队长Id
     */
    private Long userId;

    /**
     * 队伍状态 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;


    // ========== 推荐相关信息（核心）==========

    /**
     * 综合推荐得分(0-1之间)
     * 分数越高越推荐
     */
    private Double score;

    /**
     * 同城成员比例(0-1之间)
     * 值越高越推荐
     */
    private Double sameCityRatio;

    /**
     * 同省成员比例
     */
    private Double sameProvinceRatio;

    /**
     * 标签相似度(0-1之间)
     * 复用AlgorithmUtils中的算法
     */
    private Double tagSimilarity;


    // ========== 位置分布信息（可选，增强用户体验）==========

    /**
     * 队伍成员城市分布，使用Map<String, Integer>存储，key为城市名称，value为数量
     */
    private Map<String, Integer> cityDistribution;

    /**
     * 推荐理由
     * 例如：该队伍中有50%的成员在成都，相似匹配度70%左右
     */
    private String recommendReason;
}
