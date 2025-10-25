package com.yupi.usercentre.model.dto;

import com.yupi.usercentre.model.domain.Team;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 队伍得分DTO -> 只用于Service层内部计算和排序，不返回给前端
 */
@Data
public class TeamScoreDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍对象
     */
    private Team team;

    /**
     * 综合得分
     */
    private Double score;

    /**
     * 同城成员比例
     */
    private Double sameCityRatio;

    /**
     * 同省成员比例
     */
    private Double sameProvinceRatio;

    /**
     * 标签相似度
     */
    private Double tagSimilarity;

    /**
     * 城市分布
     */
    private Map<String, Integer> cityDistribution;
}
