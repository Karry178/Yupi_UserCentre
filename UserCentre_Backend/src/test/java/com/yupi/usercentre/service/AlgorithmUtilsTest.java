package com.yupi.usercentre.service;

import com.yupi.usercentre.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class AlgorithmUtilsTest {

    /**
     * 计算两个字符串的最小编辑距离（Levenshtein Distance）
     * 使用动态规划算法
     * @return 最小编辑距离
     */
    @Test
    void test(){
        String word1 = "你是狗";
        String word2 = "你不是狗了";
        String word3 = "你到底是不是狗";
        int distance12 = AlgorithmUtils.minDistance(word1, word2);
        int distance13 = AlgorithmUtils.minDistance(word1, word3);
        System.out.println(distance12);
        System.out.println(distance13);
    }


    @Test
    void testCompareTags(){
        List<String> tagList1 = Arrays.asList("java", "大一", "男");
        List<String> tagList2 = Arrays.asList("java", "大二", "女");
        List<String> tagList3 = Arrays.asList("Python", "大二", "女");
        int distance12 = AlgorithmUtils.minDistance(tagList1, tagList2);
        int distance13 = AlgorithmUtils.minDistance(tagList1, tagList3);
        System.out.println(distance12);
        System.out.println(distance13);
    }
}