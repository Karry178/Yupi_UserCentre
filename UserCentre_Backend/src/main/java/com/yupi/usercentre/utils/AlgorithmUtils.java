package com.yupi.usercentre.utils;

import java.util.List;
import java.util.Objects;

/**
 * 算法工具类
 */
public class AlgorithmUtils {
    
    /**
     * 计算两个字符串的最小编辑距离（Levenshtein Distance）
     * 使用动态规划算法
     * @param word1 第一个字符串
     * @param word2 第二个字符串
     * @return 最小编辑距离
     */
    public static int minDistance(String word1, String word2){
        int n = word1.length();
        int m = word2.length();
        
        if(n * m == 0)
            return n + m;
        
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++){
            d[i][0] = i;
        }
        
        for (int j = 0; j < m + 1; j++){
            d[0][j] = j;
        }
        
        for (int i = 1; i < n + 1; i++){
            for (int j = 1; j < m + 1; j++){
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (word1.charAt(i - 1) != word2.charAt(j - 1))
                    left_down += 1;
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }
    



    /**
     * 计算两组标签的最小编辑距离（Levenshtein Distance）
     * 使用动态规划算法
     * @param tagList1 第一个标签
     * @param tagList2 第二个标签
     * @return 最小编辑距离
     *
     * 功能：
     *     计算将 word1 转换为 word2 所需的最少操作次数（插入、删除、替换字符）
     * 算法核心：
         * 时间复杂度： O(n × m)
         * 空间复杂度： O(n × m)
     * 方法： 动态规划
     * 代码逻辑：
     * 初始化二维 DP 数组 d[n+1][m+1]
     * 边界条件：第一行和第一列分别为 0~m 和 0~n
     * 状态转移：
         * left = 删除操作
         * down = 插入操作
         * left_down = 替换操作（字符相同则无需操作）
     * 取三种操作的最小值
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2){
        int n = tagList1.size();
        int m = tagList2.size();
        
        if(n * m == 0)
            return n + m;
        
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }
        
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (!Objects.equals(tagList1.get(i - 1),tagList2.get(j - 1)))
                    left_down += 1;
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }
    
}
